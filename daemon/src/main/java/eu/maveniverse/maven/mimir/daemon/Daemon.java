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
import com.google.inject.Provides;
import eu.maveniverse.maven.mimir.node.daemon.DaemonConfig;
import eu.maveniverse.maven.mimir.shared.Config;
import eu.maveniverse.maven.mimir.shared.checksum.ChecksumAlgorithmFactory;
import eu.maveniverse.maven.mimir.shared.impl.checksum.ChecksumAlgorithmFactoryAdapter;
import eu.maveniverse.maven.mimir.shared.node.RemoteNode;
import eu.maveniverse.maven.mimir.shared.node.RemoteNodeFactory;
import eu.maveniverse.maven.mimir.shared.node.SystemNode;
import eu.maveniverse.maven.mimir.shared.node.SystemNodeFactory;
import java.io.Closeable;
import java.io.IOException;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import org.eclipse.aether.internal.impl.checksum.Md5ChecksumAlgorithmFactory;
import org.eclipse.aether.internal.impl.checksum.Sha1ChecksumAlgorithmFactory;
import org.eclipse.aether.internal.impl.checksum.Sha256ChecksumAlgorithmFactory;
import org.eclipse.aether.internal.impl.checksum.Sha512ChecksumAlgorithmFactory;
import org.eclipse.sisu.space.BeanScanning;
import org.eclipse.sisu.space.SpaceModule;
import org.eclipse.sisu.space.URLClassSpace;
import org.eclipse.sisu.wire.WireModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
public class Daemon implements Closeable {
    private static final Logger logger = LoggerFactory.getLogger(Daemon.class);

    public static void main(String[] args) {
        try {
            Config config = Config.defaults()
                    .propertiesPath(Path.of("daemon.properties"))
                    .build();

            DaemonConfig daemonConfig = DaemonConfig.with(config);
            Daemon daemon = Guice.createInjector(new WireModule(
                            new AbstractModule() {
                                @Override
                                protected void configure() {
                                    bind(Config.class).toInstance(DaemonConfig.toDaemonProcessConfig(config));
                                    bind(DaemonConfig.class).toInstance(daemonConfig);
                                }

                                @Provides
                                @Singleton
                                public Map<String, ChecksumAlgorithmFactory> checksumAlgorithms() {
                                    HashMap<String, ChecksumAlgorithmFactory> checksumAlgorithms = new HashMap<>();
                                    checksumAlgorithms.put(
                                            Md5ChecksumAlgorithmFactory.NAME,
                                            new ChecksumAlgorithmFactoryAdapter(new Md5ChecksumAlgorithmFactory()));
                                    checksumAlgorithms.put(
                                            Sha1ChecksumAlgorithmFactory.NAME,
                                            new ChecksumAlgorithmFactoryAdapter(new Sha1ChecksumAlgorithmFactory()));
                                    checksumAlgorithms.put(
                                            Sha256ChecksumAlgorithmFactory.NAME,
                                            new ChecksumAlgorithmFactoryAdapter(new Sha256ChecksumAlgorithmFactory()));
                                    checksumAlgorithms.put(
                                            Sha512ChecksumAlgorithmFactory.NAME,
                                            new ChecksumAlgorithmFactoryAdapter(new Sha512ChecksumAlgorithmFactory()));
                                    return checksumAlgorithms;
                                }
                            },
                            new SpaceModule(
                                    new URLClassSpace(Daemon.class.getClassLoader()), BeanScanning.INDEX, true)))
                    .getInstance(Daemon.class);

            Runtime.getRuntime().addShutdownHook(new Thread(daemon::close));
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    @Named
    public static final class SystemNodeProvider implements Provider<SystemNode> {
        private final SystemNode systemNode;

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
        public SystemNode get() {
            return systemNode;
        }
    }

    private final ServerSocketChannel serverSocketChannel;
    private final ExecutorService executor;
    private final SystemNode systemNode;
    private final List<RemoteNode> remoteNodes;

    @Inject
    public Daemon(
            Config config,
            DaemonConfig daemonConfig,
            SystemNode systemNode,
            Map<String, RemoteNodeFactory> remoteNodeFactories,
            Map<String, ChecksumAlgorithmFactory> checksumAlgorithmFactories)
            throws IOException {
        requireNonNull(config, "config");
        requireNonNull(daemonConfig, "daemonConfig");
        requireNonNull(systemNode, "systemNode");
        requireNonNull(remoteNodeFactories, "remoteNodeFactories");

        this.systemNode = systemNode;
        ArrayList<RemoteNode> nds = new ArrayList<>();
        for (RemoteNodeFactory remoteNodeFactory : remoteNodeFactories.values()) {
            Optional<? extends RemoteNode> node = remoteNodeFactory.createNode(config);
            node.ifPresent(nds::add);
        }
        nds.sort(Comparator.comparing(RemoteNode::distance));
        this.remoteNodes = List.copyOf(nds);
        this.executor = Executors.newVirtualThreadPerTaskExecutor();

        Path socketPath = daemonConfig.socketPath();
        // make sure socket is deleted once daemon exits
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                Files.deleteIfExists(socketPath);
            } catch (IOException e) {
                logger.warn("Failed to delete socket path: {}", socketPath, e);
            }
        }));

        UnixDomainSocketAddress socketAddress = UnixDomainSocketAddress.of(socketPath);
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open(StandardProtocolFamily.UNIX);
        serverSocketChannel.configureBlocking(true);
        serverSocketChannel.bind(socketAddress);

        this.serverSocketChannel = serverSocketChannel;
        logger.info("Mimir Daemon {} started", config.mimirVersion().orElse("UNKNOWN"));
        logger.info("  PID: {}", ProcessHandle.current().pid());
        logger.info("  Properties: {}", config.basedir().resolve(config.propertiesPath()));
        logger.info("  Supported checksums: {}", checksumAlgorithmFactories.keySet());
        logger.info("  Socket: {}", socketAddress);
        logger.info("  System Node: {}", systemNode);
        logger.info("  Using checksums: {}", systemNode.checksumAlgorithms());
        logger.info("  {} remote node(s):", remoteNodes.size());
        for (RemoteNode node : this.remoteNodes) {
            logger.info("    {}", node);
        }

        executor.submit(() -> {
            try {
                while (serverSocketChannel.isOpen()) {
                    SocketChannel socketChannel = serverSocketChannel.accept();
                    executor.submit(new DaemonServer(socketChannel, systemNode, remoteNodes, this::close));
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
                serverSocketChannel.close();
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
        } finally {
            logger.info("Daemon stopped");
        }
    }
}
