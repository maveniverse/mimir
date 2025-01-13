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
import eu.maveniverse.maven.mimir.shared.Config;
import eu.maveniverse.maven.mimir.shared.node.LocalNode;
import eu.maveniverse.maven.mimir.shared.node.LocalNodeFactory;
import eu.maveniverse.maven.mimir.shared.node.Node;
import eu.maveniverse.maven.mimir.shared.node.NodeFactory;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import org.eclipse.sisu.space.BeanScanning;
import org.eclipse.sisu.space.SpaceModule;
import org.eclipse.sisu.space.URLClassSpace;
import org.eclipse.sisu.wire.WireModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
public class Daemon implements AutoCloseable {
    public static void main(String[] args) {
        Config config =
                Config.defaults().propertiesPath(Path.of("daemon.properties")).build();

        Daemon daemon = Guice.createInjector(new WireModule(
                        new AbstractModule() {
                            @Override
                            protected void configure() {
                                bind(Config.class).toInstance(config);
                            }
                        },
                        new SpaceModule(new URLClassSpace(Daemon.class.getClassLoader()), BeanScanning.INDEX, true)))
                .getInstance(Daemon.class);

        Runtime.getRuntime().addShutdownHook(new Thread(daemon::close));
    }

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final ServerSocketChannel serverSocketChannel;
    private final ExecutorService executor;
    private final LocalNode localNode;
    private final List<Node> nodes;

    @Inject
    public Daemon(Config config, LocalNodeFactory localNodeFactory, Map<String, NodeFactory> nodeFactories)
            throws IOException {
        requireNonNull(config, "config");

        this.localNode = localNodeFactory.createLocalNode(config);
        ArrayList<Node> nds = new ArrayList<>();
        for (NodeFactory nodeFactory : nodeFactories.values()) {
            Optional<Node> node = nodeFactory.createNode(config, localNode);
            node.ifPresent(nds::add);
        }
        nds.sort(Comparator.comparing(Node::distance));
        this.nodes = List.copyOf(nds);
        this.executor = Executors.newFixedThreadPool(15);

        DaemonConfig cfg = DaemonConfig.with(config);
        Path socketPath = cfg.socketPath();
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
        logger.info("Mimir Daemon {} started", config.mimirVersion());
        logger.info("  Socket: {}", socketAddress);
        logger.info("  Local Node: {}", localNode);
        logger.info("  {} node(s):", nodes.size());
        for (Node node : this.nodes) {
            logger.info("    {} (d={})", node.name(), node.distance());
        }

        executor.submit(() -> {
            try {
                while (true) {
                    SocketChannel socketChannel = serverSocketChannel.accept();
                    executor.submit(new UdsNodeServer(socketChannel, localNode, nodes));
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
            for (Node node : this.nodes) {
                try {
                    node.close();
                } catch (Exception e) {
                    logger.warn("Error closing node", e);
                }
            }
            try {
                localNode.close();
            } catch (Exception e) {
                logger.warn("Error closing local node", e);
            }
        } finally {
            logger.info("Daemon stopped");
        }
    }
}
