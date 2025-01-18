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
import eu.maveniverse.maven.mimir.shared.node.RemoteNode;
import eu.maveniverse.maven.mimir.shared.node.RemoteNodeFactory;
import java.io.IOException;
import java.io.UncheckedIOException;
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
@Named(DaemonConfig.NAME)
public class DaemonNodeFactory implements RemoteNodeFactory {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public Optional<RemoteNode> createRemoteNode(Config config, LocalNode localNode) throws IOException {
        DaemonConfig cfg = DaemonConfig.with(config);
        if (!cfg.enabled()) {
            logger.debug("Mimir daemon not enabled");
            return Optional.empty();
        }
        if (!Files.exists(cfg.socketPath())) {
            if (cfg.autostart()) {
                logger.debug("Mimir daemon is not running, starting it");
                Process daemon = startDaemon(config.basedir(), cfg);
                if (daemon == null) {
                    return Optional.empty();
                }
                logger.info("Mimir daemon started (pid={})", daemon.pid());
            }
        }
        return Optional.of(new DaemonNode(() -> {
            try {
                return new DaemonNode.Handle(SocketChannel.open(UnixDomainSocketAddress.of(cfg.socketPath())));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }));
    }

    private Process startDaemon(Path basedir, DaemonConfig config) throws IOException {
        String daemonJarName = config.daemonJarName();
        String daemonLogName = config.daemonLogName();
        Path daemonJar = basedir.resolve(daemonJarName);
        Path daemonLog = basedir.resolve(daemonLogName);
        if (Files.isRegularFile(daemonJar)) {
            String java = Path.of(System.getProperty("java.home"))
                    .resolve("bin")
                    .resolve(System.getProperty("os.name", "unknown").startsWith("Windows") ? "java.exe" : "java")
                    .toString();
            ProcessBuilder pb = new ProcessBuilder()
                    .directory(basedir.toFile())
                    .redirectError(daemonLog.toFile())
                    .redirectOutput(daemonLog.toFile())
                    .command(java, "-jar", daemonJarName);
            Process p = pb.start();
            try {
                // not the nicest, but JGroups will also sleep 1s to discover cluster
                Thread.sleep(1500);
            } catch (InterruptedException e) {
                throw new IOException("Interrupted", e);
            }
            return p;
        }
        logger.warn("Mimir daemon is not present; cannot start it");
        return null;
    }
}
