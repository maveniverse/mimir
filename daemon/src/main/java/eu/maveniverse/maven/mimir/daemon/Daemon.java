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
import eu.maveniverse.maven.mimir.daemon.protocol.Handle;
import eu.maveniverse.maven.mimir.daemon.protocol.Request;
import eu.maveniverse.maven.mimir.daemon.protocol.Session;
import eu.maveniverse.maven.mimir.shared.Config;
import eu.maveniverse.maven.mimir.shared.node.RemoteNode;
import eu.maveniverse.maven.mimir.shared.node.RemoteNodeFactory;
import eu.maveniverse.maven.mimir.shared.node.SystemNode;
import eu.maveniverse.maven.mimir.shared.node.SystemNodeFactory;
import eu.maveniverse.maven.shared.core.component.ComponentSupport;
import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.AsynchronousCloseException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Predicate;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import org.eclipse.aether.spi.connector.checksum.ChecksumAlgorithmFactory;
import org.eclipse.sisu.space.BeanScanning;
import org.eclipse.sisu.space.SpaceModule;
import org.eclipse.sisu.space.URLClassSpace;
import org.eclipse.sisu.wire.WireModule;

@Named
@Singleton
public class Daemon extends ComponentSupport implements Closeable {
    static {
        // make Slf4j-simple go for stdout and not default stderr (unless re-configured by user)
        if (System.getProperty("org.slf4j.simpleLogger.logFile") == null) {
            System.setProperty("org.slf4j.simpleLogger.logFile", "System.out");
        }
    }

    public static void main(String[] args) {
        try {
            Config config = Config.daemonDefaults().build();

            DaemonConfig daemonConfig = DaemonConfig.with(config);
            Daemon daemon = Guice.createInjector(new WireModule(
                            new AbstractModule() {
                                @Override
                                protected void configure() {
                                    bind(Config.class).toInstance(config);
                                    bind(DaemonConfig.class).toInstance(daemonConfig);
                                }
                            },
                            new SpaceModule(
                                    new URLClassSpace(Daemon.class.getClassLoader()), BeanScanning.INDEX, true)))
                    .getInstance(Daemon.class);

            Runtime.getRuntime().addShutdownHook(new Thread(daemon::close));
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
    }

    @Named
    public static final class SystemNodeProvider implements Provider<SystemNode<?>> {
        private final SystemNode<?> systemNode;

        @Inject
        public SystemNodeProvider(
                Config config, DaemonConfig daemonConfig, Map<String, SystemNodeFactory> systemNodeFactories)
                throws IOException {
            requireNonNull(systemNodeFactories, "systemNodeFactories");
            SystemNodeFactory systemNodeFactory = systemNodeFactories.get(daemonConfig.systemNode());
            if (systemNodeFactory == null) {
                throw new IllegalArgumentException("Unknown system node: " + daemonConfig.systemNode());
            }
            this.systemNode = systemNodeFactory.createNode(config);
        }

        @Override
        public SystemNode<?> get() {
            return systemNode;
        }
    }

    private final Config config;
    private final DaemonConfig daemonConfig;
    private final Handle.ServerHandle serverHandle;
    private final ExecutorService executor;
    private final SystemNode<?> systemNode;
    private final List<RemoteNode<?>> remoteNodes;

    @Inject
    public Daemon(
            Config config,
            DaemonConfig daemonConfig,
            SystemNode<?> systemNode,
            Map<String, RemoteNodeFactory> remoteNodeFactories,
            Map<String, ChecksumAlgorithmFactory> checksumAlgorithmFactories)
            throws IOException {
        this.config = requireNonNull(config, "config");
        this.daemonConfig = requireNonNull(daemonConfig, "daemonConfig");
        requireNonNull(systemNode, "systemNode");
        requireNonNull(remoteNodeFactories, "remoteNodeFactories");

        this.systemNode = systemNode;
        ArrayList<RemoteNode<?>> nds = new ArrayList<>();
        for (RemoteNodeFactory remoteNodeFactory : remoteNodeFactories.values()) {
            Optional<? extends RemoteNode<?>> node = remoteNodeFactory.createNode(config);
            node.ifPresent(nds::add);
        }
        nds.sort(Comparator.comparing(RemoteNode::distance));
        this.remoteNodes = List.copyOf(nds);
        // Java 21: this.executor = Executors.newVirtualThreadPerTaskExecutor();
        this.executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() - 1);

        Path socketPath = daemonConfig.socketPath();
        // make sure socket is deleted once daemon exits
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                Files.deleteIfExists(socketPath);
            } catch (IOException e) {
                logger.warn("Failed to delete socket path: {}", socketPath, e);
            }
        }));
        this.serverHandle = Handle.serverDomainSocket(socketPath);

        logger.info("Mimir Daemon {} started", config.mimirVersion().orElse("UNKNOWN"));
        logger.info("  PID: {}", ProcessHandle.current().pid());
        logger.info("  Properties: {}", config.basedir().resolve(config.propertiesPath()));
        logger.info("  Supported checksums: {}", checksumAlgorithmFactories.keySet());
        logger.info("  Socket: {}", socketPath);
        logger.info("  System Node: {}", systemNode);
        logger.info("  Using checksums: {}", systemNode.checksumAlgorithms());
        logger.info("  {} remote node(s):", remoteNodes.size());
        for (RemoteNode<?> node : this.remoteNodes) {
            logger.info("    {}", node);
        }

        HashMap<String, String> daemonData = new HashMap<>();
        daemonData.put(Session.DAEMON_PID, Long.toString(ProcessHandle.current().pid()));
        daemonData.put(Session.DAEMON_VERSION, config.mimirVersion().orElse("UNKNOWN"));

        Predicate<Request> clientPredicate =
                req -> Objects.equals(req.requireData(Session.NODE_VERSION), daemonData.get(Session.DAEMON_VERSION));

        executor.submit(() -> {
            try {
                while (serverHandle.isOpen()) {
                    Handle handle = serverHandle.accept();
                    executor.submit(new DaemonServer(
                            handle, daemonData, systemNode, remoteNodes, clientPredicate, this::close));
                }
            } catch (AsynchronousCloseException ignored) {
                // we are done
            } catch (Exception e) {
                logger.error("Error while accepting client connection", e);
            }
        });
    }

    @Override
    public void close() {
        try {
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
            for (RemoteNode<?> node : remoteNodes) {
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
        } finally {
            logger.info("Daemon stopped");
        }
    }
}
