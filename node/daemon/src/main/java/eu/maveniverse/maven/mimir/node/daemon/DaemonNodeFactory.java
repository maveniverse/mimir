/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.node.daemon;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.mimir.shared.Config;
import eu.maveniverse.maven.mimir.shared.node.LocalNodeFactory;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import org.eclipse.aether.spi.connector.checksum.ChecksumAlgorithmFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@Named(DaemonConfig.NAME)
public class DaemonNodeFactory implements LocalNodeFactory {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final Map<String, ChecksumAlgorithmFactory> checksumAlgorithmFactories;

    @Inject
    public DaemonNodeFactory(Map<String, ChecksumAlgorithmFactory> checksumAlgorithmFactories) {
        this.checksumAlgorithmFactories = requireNonNull(checksumAlgorithmFactories, "checksumAlgorithmFactories");
    }

    @Override
    public DaemonNode createNode(Config config) throws IOException {
        DaemonConfig cfg = DaemonConfig.with(config);
        if (!Files.exists(cfg.socketPath())) {
            if (cfg.autostart()) {
                logger.debug("Mimir daemon is not running, starting it");
                Process daemon = startDaemon(config.basedir(), config, cfg);
                if (daemon == null) {
                    throw new IOException("Mimir daemon could not be started");
                }
                logger.info("Mimir daemon started (pid={})", daemon.pid());
            }
        }
        HashMap<String, String> clientData = new HashMap<>();
        clientData.put("node.version", config.mimirVersion().orElse("UNKNOWN"));
        clientData.put("node.pid", Long.toString(ProcessHandle.current().pid()));
        return new DaemonNode(clientData, cfg.socketPath(), checksumAlgorithmFactories, cfg.autostop());
    }

    private Process startDaemon(Path basedir, Config config, DaemonConfig daemonConfig) throws IOException {
        String daemonJarName = daemonConfig.daemonJarName();
        String daemonLogName = daemonConfig.daemonLogName();
        Path daemonJar = basedir.resolve(daemonJarName);
        Path daemonLog = basedir.resolve(daemonLogName);
        if (Files.isRegularFile(daemonJar)) {
            String java = daemonConfig
                    .daemonJavaHome()
                    .resolve("bin")
                    .resolve(
                            config.effectiveProperties()
                                            .getOrDefault("os.name", "unknown")
                                            .startsWith("Windows")
                                    ? "java.exe"
                                    : "java")
                    .toString();
            ProcessBuilder pb = new ProcessBuilder().directory(basedir.toFile()).redirectOutput(daemonLog.toFile());

            ArrayList<String> command = new ArrayList<>();
            command.add(java);
            if (daemonConfig.passOnUserHome()) {
                command.add("-Duser.home=" + System.getProperty("user.home"));
            }
            command.add("-jar");
            command.add(daemonJarName);

            pb.command(command);
            Process p = pb.start();
            try {
                while (p.isAlive() && !Files.exists(daemonConfig.socketPath())) {
                    logger.debug("Waiting for socket to open");
                    Thread.sleep(500);
                }
            } catch (InterruptedException e) {
                throw new IOException("Interrupted", e);
            }
            if (p.isAlive()) {
                return p;
            } else {
                throw new IOException("Failed to start daemon; check daemon logs in " + daemonConfig.daemonLogName());
            }
        }
        logger.warn("Mimir daemon is not present; cannot start it");
        return null;
    }
}
