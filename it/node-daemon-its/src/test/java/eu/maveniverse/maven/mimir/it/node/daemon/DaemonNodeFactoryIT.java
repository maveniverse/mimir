/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.it.node.daemon;

import static org.junit.jupiter.api.Assertions.assertTrue;

import eu.maveniverse.maven.mimir.node.daemon.DaemonNode;
import eu.maveniverse.maven.mimir.node.daemon.DaemonNodeConfig;
import eu.maveniverse.maven.mimir.node.daemon.DaemonNodeFactory;
import eu.maveniverse.maven.mimir.shared.SessionConfig;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Optional;
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
    void concurrentStart(@TempDir Path tempDir) throws Exception {
        Path baseDir = tempDir.resolve("mimir");
        Files.createDirectories(baseDir);

        SessionConfig sessionConfig = SessionConfig.daemonDefaults()
                .basedir(baseDir)
                .setUserProperty("mimir.daemon.passOnBasedir", "true")
                .setUserProperty("mimir.daemon.debug", "false")
                .build();

        DaemonNodeConfig daemonNodeConfig = DaemonNodeConfig.with(sessionConfig);
        Path daemonJar = daemonNodeConfig.daemonJar();
        Path daemonLog = daemonNodeConfig.daemonLog();

        // copy JAR to place; like it was resolved and copied there by extension3
        Files.copy(
                Path.of(System.getProperty("daemon.jar.path")),
                daemonJar,
                StandardCopyOption.REPLACE_EXISTING);

        TestLocker locker = new TestLocker();
        DaemonNodeFactory factory1 = new DaemonNodeFactory(locker);
        DaemonNodeFactory factory2 = new DaemonNodeFactory(locker);
        DaemonNodeFactory factory3 = new DaemonNodeFactory(locker);

        try {
            Optional<DaemonNode> daemonNode1 = factory1.createLocalNode(sessionConfig);
            Optional<DaemonNode> daemonNode2 = factory2.createLocalNode(sessionConfig);
            Optional<DaemonNode> daemonNode3 = factory3.createLocalNode(sessionConfig);

            assertTrue(daemonNode1.isPresent());
            assertTrue(daemonNode2.isPresent());
            assertTrue(daemonNode3.isPresent());
        } catch (Exception ex) {
            System.out.println(Files.readAllLines(daemonLog));
            throw ex;
        }
    }
}
