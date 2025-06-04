/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.shared.impl;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.LoggerFactory;

/**
 * In pre-21 we use plain old thread pool.
 */
public final class Executors {
    private Executors() {}

    private static final AtomicBoolean executorWarned = new AtomicBoolean(false);

    public static ExecutorService executorService() {
        // Mimir targets dev Workstations; they are usually multicore; on low-end systems see below for configuration
        if (Runtime.getRuntime().availableProcessors() < 3) {
            if (executorWarned.compareAndSet(false, true)) {
                LoggerFactory.getLogger(Executors.class)
                        .warn("Low-end hardware/VM; use `mimir.session.localNode=file` in mimir.properties instead");
            }
        }
        // no need for more than 12; but recommended is to use Mimir in Java 21+
        return java.util.concurrent.Executors.newFixedThreadPool(
                Math.max(1, Math.min(12, Runtime.getRuntime().availableProcessors() - 1)));
    }
}
