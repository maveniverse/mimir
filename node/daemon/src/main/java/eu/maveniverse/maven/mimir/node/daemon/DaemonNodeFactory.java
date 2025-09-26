/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.node.daemon;

import eu.maveniverse.maven.mimir.daemon.protocol.Session;
import eu.maveniverse.maven.mimir.shared.SessionConfig;
import eu.maveniverse.maven.mimir.shared.node.LocalNodeFactory;
import eu.maveniverse.maven.shared.core.component.ComponentSupport;
import eu.maveniverse.maven.shared.core.fs.DirectoryLocker;
import eu.maveniverse.maven.shared.core.fs.FileUtils;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;
import javax.inject.Named;
import javax.inject.Singleton;
import org.eclipse.aether.RepositorySystemSession;

@Singleton
@Named(DaemonNodeConfig.NAME)
public class DaemonNodeFactory extends ComponentSupport implements LocalNodeFactory<DaemonNode> {
    @Override
    public Optional<DaemonNode> createLocalNode(SessionConfig sessionConfig) throws IOException {
        DaemonNodeConfig cfg = DaemonNodeConfig.with(sessionConfig);
        if (tryLock(cfg)) {
            Files.deleteIfExists(cfg.socketPath());
            if (cfg.autostart()) {
                logger.debug("Mimir daemon is not running, starting it");
                Process daemon = startDaemon(cfg);
                logger.info("Mimir daemon started (pid={})", daemon.pid());
            } else {
                throw new IOException("Mimir daemon does not run and autostart is disabled; start daemon manually");
            }
        } else {
            if (!Files.exists(cfg.socketPath())) {
                waitForSocket(cfg);
            }
        }

        // at this point socket must exist
        if (!Files.exists(cfg.socketPath())) {
            throw new IOException("Mimir daemon socket not found");
        }
        HashMap<String, String> clientData = new HashMap<>();
        clientData.put(Session.NODE_PID, Long.toString(ProcessHandle.current().pid()));
        clientData.put(Session.NODE_VERSION, sessionConfig.mimirVersion());
        if (cfg.config().repositorySystemSession().isPresent()) {
            RepositorySystemSession session =
                    cfg.config().repositorySystemSession().orElseThrow();
            clientData.put(
                    Session.LRM_PATH,
                    FileUtils.canonicalPath(
                                    session.getLocalRepository().getBasedir().toPath())
                            .toString());
        }
        try {
            return Optional.of(new DaemonNode(clientData, cfg, cfg.autostop()));
        } catch (IOException e) {
            mayDumpDaemonLog(cfg.daemonLog());
            throw e;
        }
    }

    /**
     * Starts damon process. This method must be entered ONLY if caller owns exclusive lock "start procedure".
     *
     * @see #tryLock(DaemonNodeConfig)
     */
    private Process startDaemon(DaemonNodeConfig cfg) throws IOException {
        long startTime = System.currentTimeMillis();
        logger.info("Starting daemon process at {}", java.time.Instant.ofEpochMilli(startTime));

        Path basedir = cfg.daemonBasedir();
        if (Files.isRegularFile(cfg.daemonJar()) && Files.isReadable(cfg.daemonJar())) {
            String java = cfg.daemonJavaHome()
                    .resolve("bin")
                    .resolve(
                            cfg.config()
                                            .effectiveProperties()
                                            .getOrDefault("os.name", "unknown")
                                            .startsWith("Windows")
                                    ? "java.exe"
                                    : "java")
                    .toString();

            ArrayList<String> command = new ArrayList<>();
            command.add(java);
            if (cfg.debug()) {
                command.add("-Dorg.slf4j.simpleLogger.defaultLogLevel=debug");
            }
            if (cfg.passOnBasedir()) {
                command.add("-Dmimir.basedir=" + basedir);
            }
            command.add("-jar");
            command.add(cfg.daemonJar().toString());

            ProcessBuilder pb = new ProcessBuilder()
                    .directory(basedir.toFile())
                    .redirectOutput(cfg.daemonLog().toFile())
                    .redirectError(
                            ProcessBuilder.Redirect.appendTo(cfg.daemonLog().toFile()))
                    .command(command);

            logger.debug("Starting daemon with command: {}", command);
            logger.debug("Working directory: {}", basedir);
            logger.debug("Log file: {}", cfg.daemonLog());
            logger.debug("Socket path: {}", cfg.socketPath());

            // Release lock before starting daemon so daemon can acquire it
            long unlockStart = System.currentTimeMillis();
            logger.info("Releasing daemon startup lock... ({}ms elapsed)", unlockStart - startTime);
            unlock(cfg);
            long unlockEnd = System.currentTimeMillis();
            logger.info("Lock released (took {}ms, {}ms total elapsed)", unlockEnd - unlockStart, unlockEnd - startTime);

            // Give a small delay to ensure lock is properly released
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted while waiting to start daemon", e);
            }

            long processStart = System.currentTimeMillis();
            logger.info("Starting daemon process... ({}ms elapsed)", processStart - startTime);
            Process p = pb.start();
            long processEnd = System.currentTimeMillis();
            logger.info("Daemon process started (took {}ms, {}ms total elapsed)", processEnd - processStart, processEnd - startTime);
            try {
                long waitStart = System.currentTimeMillis();
                logger.info("Waiting for daemon socket... ({}ms elapsed)", waitStart - startTime);
                waitForSocket(cfg);
                long waitEnd = System.currentTimeMillis();
                logger.info("Daemon socket ready (took {}ms, {}ms total elapsed)", waitEnd - waitStart, waitEnd - startTime);
            } catch (IOException e) {
                p.destroy();
                throw e;
            }
            if (p.isAlive()) {
                long totalTime = System.currentTimeMillis() - startTime;
                logger.info("Daemon startup completed successfully (total time: {}ms)", totalTime);
                return p;
            } else {
                mayDumpDaemonLog(cfg.daemonLog());
                throw new IOException("Failed to start daemon; check daemon logs in " + cfg.daemonLog());
            }
        } else {
            if (!Files.exists(cfg.daemonJar())) {
                throw new IOException("Mimir daemon JAR not found: " + cfg.daemonJar());
            } else if (!Files.isRegularFile(cfg.daemonJar())) {
                throw new IOException("Mimir daemon JAR is not a regular file: " + cfg.daemonJar());
            } else if (!Files.isReadable(cfg.daemonJar())) {
                throw new IOException("Mimir daemon JAR is not readable: " + cfg.daemonJar());
            } else {
                throw new IOException("Mimir daemon JAR cannot be used: " + cfg.daemonJar());
            }
        }
    }

    /**
     * Dumps the daemon log file for user.
     */
    private void mayDumpDaemonLog(Path daemonLog) throws IOException {
        if (Files.isRegularFile(daemonLog)) {
            try {
                String logContent = Files.readString(daemonLog);
                if (logContent.trim().isEmpty()) {
                    logger.error("Daemon log file {} exists but is empty", daemonLog);
                } else {
                    logger.error("Daemon log dump from {}:\n{}", daemonLog, logContent);
                }
            } catch (IOException e) {
                logger.error("Failed to read daemon log file {}: {}", daemonLog, e.getMessage());
            }
        } else {
            logger.error("Daemon log file {} does not exist or is not a regular file", daemonLog);
        }
    }

    /**
     * Locks the {@link DaemonNodeConfig#daemonLockDir()}. If this method returns {@code true} it means there is no
     * daemon running nor is there any other process trying to start daemon.
     * This process "owns" the start procedure alone.
     */
    private boolean tryLock(DaemonNodeConfig cfg) {
        try {
            Files.createDirectories(cfg.daemonLockDir());
            DirectoryLocker.INSTANCE.lockDirectory(cfg.daemonLockDir(), true);
            logger.debug("Successfully acquired daemon startup lock");
            return true;
        } catch (IOException e) {
            logger.debug("Failed to acquire daemon startup lock: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Unlocks the {@link DaemonNodeConfig#daemonLockDir()}.
     */
    private void unlock(DaemonNodeConfig cfg) throws IOException {
        DirectoryLocker.INSTANCE.unlockDirectory(cfg.daemonLockDir());
    }

    /**
     * The method will wait {@link DaemonNodeConfig#autostartDuration()} time for socket to become available.
     * Precondition: socket does not exist.
     * Exit condition: socket exist.
     * Fail condition: time passes and socket not exist.
     */
    private void waitForSocket(DaemonNodeConfig cfg) throws IOException {
        Instant startingUntil = Instant.now().plus(cfg.autostartDuration());
        long waitStart = System.currentTimeMillis();
        logger.info("Waiting for socket {} to become available until {} (timeout: {})",
            cfg.socketPath(), startingUntil, cfg.autostartDuration());

        int attempts = 0;
        try {
            while (!Files.exists(cfg.socketPath())) {
                attempts++;
                if (Instant.now().isAfter(startingUntil)) {
                    logger.error(
                            "Timeout waiting for daemon socket after {} attempts over {}",
                            attempts,
                            cfg.autostartDuration());
                    mayDumpDaemonLog(cfg.daemonLog());
                    throw new IOException("Failed to start daemon in time " + cfg.autostartDuration() + " after "
                            + attempts + " attempts; check daemon logs in " + cfg.daemonLog());
                }

                // Log progress every 10 seconds to help with debugging
                if (attempts % 20 == 0) {
                    long elapsed = System.currentTimeMillis() - waitStart;
                    logger.info(
                            "Still waiting for daemon socket after {} attempts ({} seconds, {}ms elapsed)...",
                            attempts,
                            attempts * 0.5,
                            elapsed);
                }

                logger.debug("... waiting (attempt {})", attempts);
                Thread.sleep(500);
            }
        } catch (InterruptedException e) {
            logger.warn("Interrupted while waiting for daemon socket after {} attempts", attempts);
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while waiting for daemon socket", e);
        }

        // Final check to ensure socket exists
        long totalWaitTime = System.currentTimeMillis() - waitStart;
        logger.info("Socket wait completed after {} attempts ({}ms total wait time)", attempts, totalWaitTime);
        if (!Files.exists(cfg.socketPath())) {
            logger.error("Socket check failed after wait loop completed");
            mayDumpDaemonLog(cfg.daemonLog());
            throw new IOException(
                    "Failed to start daemon; socket not found after waiting; check daemon logs in " + cfg.daemonLog());
        }

        logger.debug("Socket {} is now available after {} attempts", cfg.socketPath(), attempts);
    }
}
