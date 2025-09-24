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
@Named(DaemonConfig.NAME)
public class DaemonNodeFactory extends ComponentSupport implements LocalNodeFactory<DaemonNode> {
    @Override
    public Optional<DaemonNode> createNode(SessionConfig sessionConfig) throws IOException {
        DaemonConfig cfg = DaemonConfig.with(sessionConfig);
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
     * @see #tryLock(DaemonConfig)
     */
    private Process startDaemon(DaemonConfig cfg) throws IOException {
        Path basedir = cfg.daemonBasedir();
        if (Files.isRegularFile(cfg.daemonJar())) {
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
                    .command(command);

            unlock(cfg);

            Process p = pb.start();
            try {
                waitForSocket(cfg);
            } catch (IOException e) {
                p.destroy();
                throw e;
            }
            if (p.isAlive()) {
                return p;
            } else {
                mayDumpDaemonLog(cfg.daemonLog());
                throw new IOException("Failed to start daemon; check daemon logs in " + cfg.daemonLog());
            }
        } else {
            throw new IOException("Mimir daemon JAR not found");
        }
    }

    /**
     * Dumps the daemon log file for user.
     */
    private void mayDumpDaemonLog(Path daemonLog) throws IOException {
        if (Files.isRegularFile(daemonLog)) {
            logger.error("Daemon log dump:\n{}", Files.readString(daemonLog));
        }
    }

    /**
     * Locks the {@link DaemonConfig#daemonLockDir()}. If this method returns {@code true} it means there is no
     * daemon running nor is there any other process trying to start daemon.
     * This process "owns" the start procedure alone.
     */
    private boolean tryLock(DaemonConfig cfg) {
        try {
            Files.createDirectories(cfg.daemonLockDir());
            DirectoryLocker.INSTANCE.lockDirectory(cfg.daemonLockDir(), true);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Unlocks the {@link DaemonConfig#daemonLockDir()}.
     */
    private void unlock(DaemonConfig cfg) throws IOException {
        DirectoryLocker.INSTANCE.unlockDirectory(cfg.daemonLockDir());
    }

    /**
     * The method will wait {@link DaemonConfig#autostartDuration()} time for socket to become available.
     * Precondition: socket does not exist.
     * Exit condition: socket exist.
     * Fail condition: time passes and socket not exist.
     */
    private void waitForSocket(DaemonConfig cfg) throws IOException {
        Instant startingUntil = Instant.now().plus(cfg.autostartDuration());
        logger.debug("Waiting for socket to become available until {}", startingUntil);
        try {
            while (!Files.exists(cfg.socketPath())) {
                if (Instant.now().isAfter(startingUntil)) {
                    mayDumpDaemonLog(cfg.daemonLog());
                    throw new IOException("Failed to start daemon in time " + cfg.autostartDuration()
                            + "; check daemon logs in " + cfg.daemonLog());
                }
                logger.debug("... waiting");
                Thread.sleep(500);
            }
        } catch (InterruptedException e) {
            throw new IOException("Interrupted", e);
        }
        if (!Files.exists(cfg.socketPath())) {
            mayDumpDaemonLog(cfg.daemonLog());
            throw new IOException("Failed to start daemon; check daemon logs in " + cfg.daemonLog());
        }
    }
}
