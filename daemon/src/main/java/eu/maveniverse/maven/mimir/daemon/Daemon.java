/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.daemon;

import static java.util.Objects.requireNonNull;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import eu.maveniverse.maven.mima.context.Context;
import eu.maveniverse.maven.mima.context.ContextOverrides;
import eu.maveniverse.maven.mima.context.MavenSystemHome;
import eu.maveniverse.maven.mima.context.MavenUserHome;
import eu.maveniverse.maven.mima.context.Runtime;
import eu.maveniverse.maven.mima.context.Runtimes;
import eu.maveniverse.maven.mima.context.internal.MavenUserHomeImpl;
import eu.maveniverse.maven.mima.runtime.shared.PreBoot;
import eu.maveniverse.maven.mimir.daemon.protocol.Handle;
import eu.maveniverse.maven.mimir.daemon.protocol.Request;
import eu.maveniverse.maven.mimir.daemon.protocol.Session;
import eu.maveniverse.maven.mimir.shared.MimirUtils;
import eu.maveniverse.maven.mimir.shared.SessionConfig;
import eu.maveniverse.maven.mimir.shared.SessionFactory;
import eu.maveniverse.maven.mimir.shared.impl.Executors;
import eu.maveniverse.maven.mimir.shared.impl.ParseUtils;
import eu.maveniverse.maven.mimir.shared.impl.node.CachingSystemNode;
import eu.maveniverse.maven.mimir.shared.node.RemoteNode;
import eu.maveniverse.maven.mimir.shared.node.RemoteNodeFactory;
import eu.maveniverse.maven.mimir.shared.node.SystemNode;
import eu.maveniverse.maven.mimir.shared.node.SystemNodeFactory;
import eu.maveniverse.maven.shared.core.component.CloseableConfigSupport;
import eu.maveniverse.maven.shared.core.fs.DirectoryLocker;
import eu.maveniverse.maven.shared.core.fs.FileUtils;
import java.io.Closeable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.channels.AsynchronousCloseException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResult;
import org.eclipse.aether.spi.connector.checksum.ChecksumAlgorithmFactory;
import org.eclipse.sisu.space.BeanScanning;
import org.eclipse.sisu.space.SpaceModule;
import org.eclipse.sisu.space.URLClassSpace;
import org.eclipse.sisu.wire.WireModule;

@Named
@Singleton
public class Daemon extends CloseableConfigSupport<DaemonConfig> implements Closeable {
    static {
        // make Slf4j-simple go for stdout and not default stderr (unless re-configured by user)
        if (System.getProperty("org.slf4j.simpleLogger.logFile") == null) {
            System.setProperty("org.slf4j.simpleLogger.logFile", "System.out");
        }
    }

    public static void main(String[] args) {
        try {
            SessionConfig sessionConfig = SessionConfig.daemonDefaults().build();
            DaemonConfig daemonConfig = DaemonConfig.with(sessionConfig);

            // lock exclusively (and early) the basedir; if other daemon tries to run here it will fail
            Files.createDirectories(daemonConfig.daemonLockDir());
            DirectoryLocker.INSTANCE.lockDirectory(daemonConfig.daemonLockDir(), true);

            // clear up possible remnants; stale socket path
            Files.deleteIfExists(daemonConfig.socketPath());

            Daemon daemon = Guice.createInjector(new WireModule(
                            new AbstractModule() {
                                @Override
                                protected void configure() {
                                    bind(SessionConfig.class).toInstance(sessionConfig);
                                    bind(DaemonConfig.class).toInstance(daemonConfig);
                                    bind(PreBoot.class)
                                            .toInstance(new PreBoot(
                                                    ContextOverrides.create()
                                                            .withUserSettings(true)
                                                            .build(),
                                                    new MavenUserHomeImpl(FileUtils.discoverUserHomeDirectory()
                                                            .resolve(".m2")),
                                                    null, // maven.home
                                                    daemonConfig.config().basedir()));
                                }
                            },
                            new SpaceModule(
                                    new URLClassSpace(Daemon.class.getClassLoader()), BeanScanning.INDEX, false)))
                    .getInstance(Daemon.class);

            java.lang.Runtime.getRuntime().addShutdownHook(new Thread(daemon::shutdown));
        } catch (Exception e) {
            e.printStackTrace(System.out);
            System.exit(1);
        }
    }

    @Named
    public static final class SystemNodeProvider implements Provider<SystemNode> {
        private final SystemNode systemNode;

        @Inject
        public SystemNodeProvider(DaemonConfig daemonConfig, Map<String, SystemNodeFactory<?>> systemNodeFactories)
                throws IOException {
            requireNonNull(systemNodeFactories, "systemNodeFactories");
            SystemNodeFactory<?> systemNodeFactory = systemNodeFactories.get(daemonConfig.systemNode());
            if (systemNodeFactory == null) {
                throw new IllegalArgumentException("Unknown system node: " + daemonConfig.systemNode());
            }
            this.systemNode = systemNodeFactory.createSystemNode(daemonConfig.config());
        }

        @Override
        public SystemNode get() {
            return systemNode;
        }
    }

    private final ExecutorService executor;
    private final SessionFactory sessionFactory;
    private final SystemNode systemNode;
    private final List<RemoteNode> remoteNodes;
    private final Handle.ServerHandle serverHandle;

    @Inject
    public Daemon(
            SessionConfig sessionConfig,
            DaemonConfig daemonConfig,
            SessionFactory sessionFactory,
            SystemNode systemNode,
            Map<String, RemoteNodeFactory<?>> remoteNodeFactories,
            Map<String, ChecksumAlgorithmFactory> checksumAlgorithmFactories)
            throws IOException {
        super(daemonConfig);
        this.sessionFactory = requireNonNull(sessionFactory, "sessionFactory");
        this.systemNode = requireNonNull(systemNode, "systemNode");
        requireNonNull(remoteNodeFactories, "remoteNodeFactories");

        ArrayList<RemoteNode> nds = new ArrayList<>();
        for (RemoteNodeFactory<?> remoteNodeFactory : remoteNodeFactories.values()) {
            Optional<? extends RemoteNode> node = remoteNodeFactory.createRemoteNode(config.config());
            node.ifPresent(nds::add);
        }
        nds.sort(Comparator.comparing(RemoteNode::distance));
        this.remoteNodes = List.copyOf(nds);
        this.executor = Executors.executorService();

        logger.info("Mimir Daemon {} started", config.config().mimirVersion());
        logger.info("  PID: {}", ProcessHandle.current().pid());
        logger.info("  Basedir: {}", config.config().basedir());
        logger.info("  Properties: {}", config.config().propertiesPath());
        logger.info("  Supported checksums: {}", checksumAlgorithmFactories.keySet());
        logger.info("  Socket: {}", daemonConfig.socketPath());
        logger.info("  System Node: {}", systemNode);
        logger.info("  Using checksums: {}", systemNode.checksumAlgorithms());
        if (remoteNodes.isEmpty()) {
            logger.info("  No remote node(s) configured");
        } else {
            logger.info("  {} remote node(s):", remoteNodes.size());
            for (RemoteNode node : this.remoteNodes) {
                logger.info("    {}", node);
            }
        }

        withResolver(this::dumpMima);

        if (daemonConfig.preSeedItself() || !daemonConfig.preSeedArtifacts().isEmpty()) {
            withResolver((s, c) -> {
                logger.info(
                        "Pre-seeding (LRM: {}; cache: {})",
                        c.repositorySystemSession().getLocalRepository().getBasedir(),
                        systemNode);
                try {
                    // redirect session localNode to our systemNode
                    Map<String, String> userProperties = new HashMap<>(sessionConfig.userProperties());
                    userProperties.put("mimir.session.localNode", systemNode.name());
                    SessionConfig sc = sessionConfig.toBuilder()
                            .userProperties(userProperties)
                            .repositorySystemSession(c.repositorySystemSession())
                            .build();
                    try (eu.maveniverse.maven.mimir.shared.Session mimirSession =
                            MimirUtils.lazyInit(c.repositorySystemSession(), () -> {
                                try {
                                    return sessionFactory.createSession(sc);
                                } catch (IOException e) {
                                    throw new UncheckedIOException(e);
                                }
                            })) {
                        CollectRequest cr;
                        DependencyResult dr;
                        if (daemonConfig.preSeedItself()) {
                            // extension
                            cr = new CollectRequest(
                                    new Dependency(
                                            new DefaultArtifact(
                                                    "eu.maveniverse.maven.mimir:extension3:" + sc.mimirVersion()),
                                            ""),
                                    Collections.singletonList(ParseUtils.CENTRAL));
                            cr.setRequestContext("mimir-daemon");
                            dr = c.repositorySystem()
                                    .resolveDependencies(c.repositorySystemSession(), new DependencyRequest(cr, null));
                            logger.info(
                                    "Pre-seeded Mimir extension ({}) from Maven Central ({} artifacts)",
                                    cr.getRoot().getArtifact(),
                                    dr.getArtifactResults().size());
                            for (ArtifactResult ar : dr.getArtifactResults()) {
                                logger.debug("  - {}", ar.getArtifact());
                            }

                            // daemon
                            // TODO: GAV! DaemonNodeConfig!
                            cr = new CollectRequest(
                                    new Dependency(
                                            new DefaultArtifact("eu.maveniverse.maven.mimir:daemon:jar:daemon:"
                                                    + sc.mimirVersion()),
                                            ""),
                                    Collections.singletonList(ParseUtils.CENTRAL));
                            cr.setRequestContext("mimir-daemon");
                            dr = c.repositorySystem()
                                    .resolveDependencies(c.repositorySystemSession(), new DependencyRequest(cr, null));
                            logger.info(
                                    "Pre-seeded Mimir daemon ({}) from Maven Central ({} artifacts)",
                                    cr.getRoot().getArtifact(),
                                    dr.getArtifactResults().size());
                            for (ArtifactResult ar : dr.getArtifactResults()) {
                                logger.debug("  - {}", ar.getArtifact());
                            }
                        }
                        for (ParseUtils.ArtifactSource source : daemonConfig.preSeedArtifacts()) {
                            if (source.artifact().getFile() != null) {
                                throw new IllegalArgumentException("Pre-seed cannot use pre-resolved artifact/file "
                                        + source.artifact().getFile());
                            }
                            cr = new CollectRequest(
                                    new Dependency(source.artifact(), ""),
                                    Collections.singletonList(source.remoteRepository()));
                            cr.setRequestContext("mimir-daemon");
                            dr = c.repositorySystem()
                                    .resolveDependencies(c.repositorySystemSession(), new DependencyRequest(cr, null));
                            logger.info(
                                    "Pre-seeded {} from {} ({} artifacts)",
                                    source.artifact(),
                                    source.remoteRepository(),
                                    dr.getArtifactResults().size());
                            for (ArtifactResult ar : dr.getArtifactResults()) {
                                logger.debug("  - {}", ar.getArtifact());
                            }
                        }
                    }
                } catch (Exception e) {
                    logger.warn("Pre-seeding failed", e);
                }
            });
        }

        HashMap<String, String> daemonData = new HashMap<>();
        daemonData.put(Session.DAEMON_PID, Long.toString(ProcessHandle.current().pid()));
        daemonData.put(Session.DAEMON_VERSION, config.config().mimirVersion());

        Predicate<Request> clientPredicate =
                req -> Objects.equals(req.requireData(Session.NODE_VERSION), daemonData.get(Session.DAEMON_VERSION));

        // open the UDS; daemon is ready
        this.serverHandle = Handle.serverDomainSocket(daemonConfig.socketPath());
        try (this.serverHandle) {
            while (serverHandle.isOpen()) {
                Handle handle = serverHandle.accept();
                executor.submit(new DaemonServer(
                        handle,
                        daemonData,
                        new CachingSystemNode(systemNode, remoteNodes),
                        clientPredicate,
                        this::shutdown));
            }
        } catch (AsynchronousCloseException ignored) {
            // we are done
        } catch (Exception e) {
            logger.error("Error while accepting client connection", e);
        } finally {
            Files.deleteIfExists(daemonConfig.socketPath());
            DirectoryLocker.INSTANCE.unlockDirectory(config.daemonLockDir());
            logger.info("Daemon stopped");
        }
    }

    public void shutdown() {
        try {
            close();
        } catch (IOException e) {
            logger.warn("Failed to close daemon", e);
        }
    }

    @Override
    protected void doClose() {
        try {
            serverHandle.close();
        } catch (Exception e) {
            logger.warn("Error closing server socket channel", e);
        }
        try {
            executor.shutdown();
        } catch (Exception e) {
            logger.warn("Error closing executor", e);
        }
        for (RemoteNode node : remoteNodes) {
            try {
                node.close();
            } catch (IOException e) {
                logger.warn("Error closing node", e);
            }
        }
        try {
            systemNode.close();
        } catch (IOException e) {
            logger.warn("Error closing local node", e);
        }
    }

    protected void withResolver(BiConsumer<Runtime, Context> resolver) {
        Runtime runtime = Runtimes.INSTANCE.getRuntime();
        try (Context context =
                runtime.create(ContextOverrides.create().withUserSettings(true).build())) {
            resolver.accept(runtime, context);
        }
    }

    protected void dumpMima(Runtime runtime, Context context) {
        logger.info("  Embeds MIMA Runtime '{}' version {}", runtime.name(), runtime.version());
        if (logger.isDebugEnabled()) {
            logger.info("MIMA dump:");
            logger.info("");
            logger.info("          Maven version {}", runtime.mavenVersion());
            logger.info("                Managed {}", runtime.managedRepositorySystem());
            logger.info("                Basedir {}", context.basedir());
            logger.info(
                    "                Offline {}",
                    context.repositorySystemSession().isOffline());

            MavenSystemHome mavenSystemHome = context.mavenSystemHome();
            logger.info("");
            logger.info(
                    "             MAVEN_HOME {}", mavenSystemHome == null ? "undefined" : mavenSystemHome.basedir());
            if (mavenSystemHome != null) {
                logger.info("           settings.xml {}", mavenSystemHome.settingsXml());
                logger.info("         toolchains.xml {}", mavenSystemHome.toolchainsXml());
            }

            MavenUserHome mavenUserHome = context.mavenUserHome();
            logger.info("");
            logger.info("              USER_HOME {}", mavenUserHome.basedir());
            logger.info("           settings.xml {}", mavenUserHome.settingsXml());
            logger.info("  settings-security.xml {}", mavenUserHome.settingsSecurityXml());
            logger.info("       local repository {}", mavenUserHome.localRepository());

            logger.info("");
            logger.info("               PROFILES");
            logger.info("                 Active {}", context.contextOverrides().getActiveProfileIds());
            logger.info("               Inactive {}", context.contextOverrides().getInactiveProfileIds());

            logger.info("");
            logger.info("    REMOTE REPOSITORIES");
            for (RemoteRepository repository : context.remoteRepositories()) {
                if (repository.getMirroredRepositories().isEmpty()) {
                    logger.info("                        {}", repository);
                } else {
                    logger.info("                        {}, mirror of", repository);
                    for (RemoteRepository mirrored : repository.getMirroredRepositories()) {
                        logger.info("                          {}", mirrored);
                    }
                }
            }
        }
    }
}
