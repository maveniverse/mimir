/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.node.daemon;

import eu.maveniverse.maven.mimir.shared.Config;
import eu.maveniverse.maven.mimir.shared.node.LocalNode;
import eu.maveniverse.maven.mimir.shared.node.Node;
import eu.maveniverse.maven.mimir.shared.node.NodeFactory;
import java.io.IOException;
import java.net.UnixDomainSocketAddress;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import javax.inject.Named;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@Named(UdsNodeConfig.NAME)
public class UdsNodeFactory implements NodeFactory {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public Optional<Node> createNode(Config config, LocalNode localNode) throws IOException {
        SocketChannel socketChannel = createSocketChannel(config);
        if (socketChannel == null) {
            return Optional.empty();
        }
        return Optional.of(new UdsNode(socketChannel));
    }

    private SocketChannel createSocketChannel(Config config) throws IOException {
        UdsNodeConfig cfg = UdsNodeConfig.with(config);
        if (!cfg.enabled()) {
            logger.info("Mimir daemon not enabled");
            return null;
        }
        if (!Files.exists(cfg.socketPath())) {
            if (cfg.autostart()) {
                logger.info("Mimir daemon is not running, starting it");
                if (!startDaemon(config)) {
                    return null;
                }
            }
        }
        return SocketChannel.open(UnixDomainSocketAddress.of(cfg.socketPath()));
    }

    private boolean startDaemon(Config config) throws IOException {
        String daemonJarName = "daemon-" + config.mimirVersion() + ".jar";
        if (Files.isRegularFile(config.basedir().resolve(daemonJarName))) {
            String java = Path.of(System.getProperty("java.home"))
                    .resolve("bin")
                    .resolve(System.getProperty("os.name", "unknown").startsWith("Windows") ? "java.exe" : "java")
                    .toString();
            Path log = config.basedir().resolve("daemon-" + config.mimirVersion() + ".log");
            ProcessBuilder pb = new ProcessBuilder()
                    .directory(config.basedir().toFile())
                    .redirectError(log.toFile())
                    .redirectOutput(log.toFile())
                    .command(java, "-jar", daemonJarName);
            pb.start();
            try {
                // not the nicest, but JGroups will also sleep 1s to discover cluster
                Thread.sleep(1500);
            } catch (InterruptedException e) {
                throw new IOException("Interrupted", e);
            }
            return true;
        }
        logger.info("Mimir daemon is not present; cannot start it");
        return false;
    }
}
