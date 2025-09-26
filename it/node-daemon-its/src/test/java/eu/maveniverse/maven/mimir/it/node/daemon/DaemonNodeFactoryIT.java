/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.it.node.daemon;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.CopyOnWriteArrayList;
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

    private SessionConfig sessionConfig;
    private SessionConfig daemonSessionConfig;
    private Path daemonJar;
    private Path daemonLog;

    private void prepareEnv(Path tempDir) throws Exception {
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

        sessionConfig = SessionConfig.defaults().basedir(baseDir).build();
        assertTrue(sessionConfig.effectiveProperties().keySet().containsAll(sessionProperties.stringPropertyNames()));

        daemonSessionConfig = SessionConfig.daemonDefaults().basedir(baseDir).build();
        assertTrue(
                daemonSessionConfig.effectiveProperties().keySet().containsAll(daemonProperties.stringPropertyNames()));

        DaemonNodeConfig daemonNodeConfig = DaemonNodeConfig.with(sessionConfig);
        daemonJar = daemonNodeConfig.daemonJar();
        daemonLog = daemonNodeConfig.daemonLog();

        // copy JAR to place; like it was resolved and copied there by extension3
        Files.copy(Path.of(System.getProperty("daemon.jar.path")), daemonJar, StandardCopyOption.REPLACE_EXISTING);
    }

    @Test
    void simple(@TempDir Path tempDir) throws Exception {
        prepareEnv(tempDir);

        TestLocker locker = new TestLocker();
        DaemonNodeFactory factory1 = new DaemonNodeFactory(locker);

        try {
            Optional<DaemonNode> dno = factory1.createLocalNode(sessionConfig);
            assertTrue(dno.isPresent());
            try (DaemonNode daemonNode = dno.orElseThrow()) {
                System.out.println("Session:");
                System.out.println(daemonNode.getSession());
                System.out.println("Daemon data:");
                System.out.println(daemonNode.getDaemonData());
            }
        } catch (Exception ex) {
            ex.printStackTrace(System.out);
        }

        System.out.println("Daemon log:");
        System.out.println(Files.readString(daemonLog));
    }

    @Test
    void concurrent(@TempDir Path tempDir) throws Exception {
        prepareEnv(tempDir);

        TestLocker locker = new TestLocker();

        int concurrency = 5;

        CountDownLatch cl = new CountDownLatch(concurrency);
        CopyOnWriteArrayList<Map<String, String>> daemonData = new CopyOnWriteArrayList<>();
        AtomicBoolean failure = new AtomicBoolean(false);

        ArrayList<Thread> threads = new ArrayList<>(concurrency);

        for (int i = 0; i < concurrency; i++) {
            threads.add(new Thread(() -> {
                try {
                    DaemonNodeFactory factory = new DaemonNodeFactory(locker);
                    Optional<DaemonNode> dno = factory.createLocalNode(sessionConfig);
                    assertTrue(dno.isPresent());
                    try (DaemonNode daemonNode = dno.orElseThrow()) {
                        daemonData.add(daemonNode.getDaemonData());
                    }
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

        // they all ended up with same daemon
        Map<String, String> first = daemonData.get(0);
        for (int i = 1; i < concurrency; i++) {
            assertEquals(first, daemonData.get(i));
        }
    }
}
