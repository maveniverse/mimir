/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.it.node.daemon;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import eu.maveniverse.maven.mimir.node.daemon.DaemonNode;
import eu.maveniverse.maven.mimir.node.daemon.DaemonNodeConfig;
import eu.maveniverse.maven.mimir.node.daemon.DaemonNodeFactory;
import eu.maveniverse.maven.mimir.shared.SessionConfig;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class DaemonNodeFactoryIT {
    /**
     * Locker implementation using {@link ReentrantReadWriteLock} as FS locking is JVM wide.
     */
    private static class TestLocker implements DaemonNodeFactory.Locker {
        private final HashMap<Path, ReentrantReadWriteLock> locks;
        private final HashMap<Path, ArrayDeque<Boolean>> steps;

        public TestLocker() {
            this.locks = new HashMap<>();
            this.steps = new HashMap<>();
        }

        @Override
        public synchronized boolean tryLock(Path directory, boolean exclusive) {
            ReentrantReadWriteLock lock = locks.computeIfAbsent(directory, d -> new ReentrantReadWriteLock());
            if (exclusive) {
                if (lock.writeLock().tryLock()) {
                    steps.computeIfAbsent(directory, d -> new ArrayDeque<>()).add(true);
                    return true;
                }
            } else {
                if (lock.readLock().tryLock()) {
                    steps.computeIfAbsent(directory, d -> new ArrayDeque<>()).add(false);
                    return true;
                }
            }
            return false;
        }

        @Override
        public synchronized void unlock(Path directory) throws IOException {
            ArrayDeque<Boolean> step = steps.get(directory);
            if (step != null && !step.isEmpty()) {
                ReentrantReadWriteLock lock = locks.get(directory);
                if (lock != null) {
                    boolean exclusive = step.pop();
                    if (exclusive) {
                        lock.writeLock().unlock();
                    } else {
                        lock.readLock().unlock();
                    }
                    return;
                }
            }
            throw new IOException("Directory: " + directory + " is not locked");
        }
    }

    @Test
    void simple(@TempDir Path tempDir) throws Exception {
        // to circumvent stupid "SocketException: Unix domain path too long"
        Path realTmp = Path.of(System.getProperty("real.java.io.tmpdir"));
        Path socketPath = realTmp.resolve(
                        "mimir.socket-" + ThreadLocalRandom.current().nextLong())
                .toAbsolutePath();

        Path baseDir = tempDir.resolve("mimir");
        Files.createDirectories(baseDir);

        Properties daemonProperties = new Properties();
        daemonProperties.setProperty("mimir.daemon.socketPath", socketPath.toString());
        try (OutputStream os = Files.newOutputStream(baseDir.resolve("daemon.properties"))) {
            daemonProperties.store(os, null);
        }

        Properties sessionProperties = new Properties();
        sessionProperties.setProperty("mimir.daemon.socketPath", socketPath.toString());
        sessionProperties.setProperty("mimir.daemon.passOnBasedir", "true");
        sessionProperties.setProperty("mimir.daemon.debug", "false");
        try (OutputStream os = Files.newOutputStream(baseDir.resolve("session.properties"))) {
            sessionProperties.store(os, null);
        }

        SessionConfig sessionConfig = SessionConfig.defaults().basedir(baseDir).build();
        assertTrue(sessionConfig.effectiveProperties().keySet().containsAll(sessionProperties.stringPropertyNames()));

        SessionConfig daemonSessionConfig =
                SessionConfig.daemonDefaults().basedir(baseDir).build();
        assertTrue(
                daemonSessionConfig.effectiveProperties().keySet().containsAll(daemonProperties.stringPropertyNames()));

        DaemonNodeConfig daemonNodeConfig = DaemonNodeConfig.with(sessionConfig);
        Path daemonJar = daemonNodeConfig.daemonJar();
        Path daemonLog = daemonNodeConfig.daemonLog();

        // copy JAR to place; like it was resolved and copied there by extension3
        Files.copy(Path.of(System.getProperty("daemon.jar.path")), daemonJar, StandardCopyOption.REPLACE_EXISTING);

        TestLocker locker = new TestLocker();
        DaemonNodeFactory factory1 = new DaemonNodeFactory(locker);

        try {
            Optional<DaemonNode> daemonNode1 = factory1.createLocalNode(sessionConfig);
            assertTrue(daemonNode1.isPresent());
            daemonNode1.orElseThrow().close();
        } catch (Exception ex) {
            ex.printStackTrace(System.out);
        }

        System.out.println("Daemon log:");
        System.out.println(Files.readString(daemonLog));
    }

    @Test
    void concurrent(@TempDir Path tempDir) throws Exception {
        // to circumvent stupid "SocketException: Unix domain path too long"
        Path realTmp = Path.of(System.getProperty("real.java.io.tmpdir"));
        Path socketPath = realTmp.resolve(
                        "mimir.socket-" + ThreadLocalRandom.current().nextLong())
                .toAbsolutePath();

        Path baseDir = tempDir.resolve("mimir");
        Files.createDirectories(baseDir);

        Properties daemonProperties = new Properties();
        daemonProperties.setProperty("mimir.daemon.socketPath", socketPath.toString());
        try (OutputStream os = Files.newOutputStream(baseDir.resolve("daemon.properties"))) {
            daemonProperties.store(os, null);
        }

        Properties sessionProperties = new Properties();
        sessionProperties.setProperty("mimir.daemon.socketPath", socketPath.toString());
        sessionProperties.setProperty("mimir.daemon.passOnBasedir", "true");
        sessionProperties.setProperty("mimir.daemon.debug", "false");
        try (OutputStream os = Files.newOutputStream(baseDir.resolve("session.properties"))) {
            sessionProperties.store(os, null);
        }

        SessionConfig sessionConfig = SessionConfig.defaults().basedir(baseDir).build();
        assertTrue(sessionConfig.effectiveProperties().keySet().containsAll(sessionProperties.stringPropertyNames()));

        SessionConfig daemonSessionConfig =
                SessionConfig.daemonDefaults().basedir(baseDir).build();
        assertTrue(
                daemonSessionConfig.effectiveProperties().keySet().containsAll(daemonProperties.stringPropertyNames()));

        DaemonNodeConfig daemonNodeConfig = DaemonNodeConfig.with(sessionConfig);
        Path daemonJar = daemonNodeConfig.daemonJar();
        Path daemonLog = daemonNodeConfig.daemonLog();

        // copy JAR to place; like it was resolved and copied there by extension3
        Files.copy(Path.of(System.getProperty("daemon.jar.path")), daemonJar, StandardCopyOption.REPLACE_EXISTING);

        TestLocker locker = new TestLocker();

        int concurrency = 5;

        CountDownLatch cl = new CountDownLatch(concurrency);
        AtomicBoolean failure = new AtomicBoolean(false);

        ArrayList<Thread> threads = new ArrayList<>(concurrency);

        for (int i = 0; i < concurrency; i++) {
            threads.add(new Thread(() -> {
                try {
                    DaemonNodeFactory factory = new DaemonNodeFactory(locker);
                    Optional<DaemonNode> daemonNode = factory.createLocalNode(sessionConfig);
                    assertTrue(daemonNode.isPresent());
                    daemonNode.orElseThrow().close();
                } catch (Exception ex) {
                    failure.set(true);
                    ex.printStackTrace(System.out);
                } finally {
                    cl.countDown();
                }
            }));
        }

        threads.forEach(Thread::start);
        cl.await();

        assertFalse(failure.get());
    }
}
