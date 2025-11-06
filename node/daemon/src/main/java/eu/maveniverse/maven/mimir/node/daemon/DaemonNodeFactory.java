/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.node.daemon;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.mimir.daemon.protocol.Handle;
import eu.maveniverse.maven.mimir.daemon.protocol.Request;
import eu.maveniverse.maven.mimir.daemon.protocol.Response;
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
import java.util.Map;
import java.util.Optional;
import javax.inject.Named;
import javax.inject.Singleton;
import org.eclipse.aether.RepositorySystemSession;

@Singleton
@Named(DaemonNodeConfig.NAME)
public class DaemonNodeFactory extends ComponentSupport implements LocalNodeFactory<DaemonNode> {
    /**
     * Component being able to lock/unlock directories.
     */
    public interface Locker {
        /**
         * Locks the passed in {@link Path} shared or exclusively. If this method returns {@code true} it means that
         * wanted directory was successfully locked in wanted mode.
         */
        boolean tryLock(Path directory, boolean exclusive) throws IOException;

        /**
         * Unlocks the passed in {@link Path}. If path was not locked, it throws; hence locking and unlocking must
         * be properly boxed.
         */
        void unlock(Path directory) throws IOException;
    }

    private final Locker locker;

    public DaemonNodeFactory() {
        this(new Locker() {
            @Override
            public boolean tryLock(Path directory, boolean exclusive) throws IOException {
                try {
                    Files.createDirectories(directory);
                    DirectoryLocker.INSTANCE.lockDirectory(directory, exclusive);
                    return true;
                } catch (IOException e) {
                    return false;
                }
            }

            @Override
            public void unlock(Path directory) throws IOException {
                DirectoryLocker.INSTANCE.unlockDirectory(directory);
            }
        });
    }

    /**
     * For testing; the {@link DirectoryLocker} makes testing within single JVM non-trivial.
     */
    public DaemonNodeFactory(Locker locker) {
        this.locker = requireNonNull(locker);
    }

    @Override
    public Optional<DaemonNode> createLocalNode(SessionConfig sessionConfig) throws IOException {
        DaemonNodeConfig cfg = DaemonNodeConfig.with(sessionConfig);
        if (locker.tryLock(cfg.daemonStarterLockDir(), true)) {
            try {
                if (locker.tryLock(cfg.daemonLockDir(), true)) {
                    // we locked both exclusively; we control everything; daemon is dead/not running
                    Files.deleteIfExists(cfg.socketPath());
                    if (cfg.autostart()) {
                        logger.debug("Mimir daemon is not running, starting it");
                        // start daemon unlocks daemonLockDir; returns when socket available
                        Process daemon = startDaemon(cfg);
                        logger.info("Mimir daemon started (pid={})", daemon.pid());
                    } else {
                        locker.unlock(cfg.daemonLockDir());
                        throw new IOException(
                                "Mimir daemon does not run and autostart is disabled; start daemon manually");
                    }
                } else {
                    // daemon may be running; but nobody is trying to start it
                    waitForSocket(cfg);
                }
            } finally {
                locker.unlock(cfg.daemonStarterLockDir());
            }
        } else {
            // someone else is trying to start it; hopefully daemon may be running soon
            waitForSocket(cfg);
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
            Handle.ClientHandle clientHandle = Handle.clientDomainSocket(cfg.socketPath());
            Map<String, String> sessionMap = null;
            Map<String, String> daemonDataMap = null;
            for (int attempt = 0; attempt < 3; attempt++) {
                // give some time to server
                Thread.sleep(100);
                try (Handle handle = clientHandle.getHandle()) {
                    handle.writeRequest(Request.hello(clientData));
                    Response helloResponse = handle.readResponse();
                    sessionMap = helloResponse.session();
                    daemonDataMap = helloResponse.data();
                    logger.debug("Hello OK {}", helloResponse.data());
                    break;
                } catch (IOException e) {
                    logger.warn("Could not HELLO with server: {}", e.getMessage());
                    sessionMap = null;
                    daemonDataMap = null;
                }
            }
            if (sessionMap == null || daemonDataMap == null) {
                clientHandle.close();
                mayDumpDaemonLog(cfg.daemonLog());
                throw new IOException("Could not connect to daemon");
            }
            return Optional.of(new DaemonNode(cfg, clientHandle, sessionMap, daemonDataMap, cfg.autostop()));
        } catch (IOException e) {
            mayDumpDaemonLog(cfg.daemonLog());
            throw e;
        } catch (InterruptedException e) {
            mayDumpDaemonLog(cfg.daemonLog());
            throw new IOException(e);
        }
    }

    /**
     * Starts damon process. This method must be entered ONLY if caller owns exclusive lock of
     * {@link DaemonNodeConfig#daemonLockDir()}, as this method will release this lock.
     *
     * @see Locker#tryLock(Path, boolean)
     * @see Locker#unlock(Path)
     */
    private Process startDaemon(DaemonNodeConfig cfg) throws IOException {
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
            cfg.localRepository().ifPresent(localRepository -> {
                command.add("-Dmimir.daemon.localRepository=" + localRepository);
            });
            command.add("-jar");
            command.add(cfg.daemonJar().toString());

            ProcessBuilder pb = new ProcessBuilder()
                    .directory(basedir.toFile())
                    .redirectOutput(cfg.daemonLog().toFile())
                    .command(command);

            locker.unlock(cfg.daemonLockDir());

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
     * The method will wait {@link DaemonNodeConfig#autostartDuration()} time for socket to become available.
     * Precondition: socket does not exist.
     * Exit condition: socket exist.
     * Fail condition: time passes and socket not exist.
     */
    private void waitForSocket(DaemonNodeConfig cfg) throws IOException {
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
