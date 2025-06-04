/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.shared.impl;

import java.util.concurrent.ExecutorService;

/**
 * In Java 21 we use virtual threads, as those are ideal for these pure IO workloads.
 */
public final class Executors {
    private Executors() {}

    public static ExecutorService executorService() {
        return java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor();
    }
}
