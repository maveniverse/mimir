/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.shared.impl;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.mimir.shared.node.Node;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class NodeSupport implements Node {
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    protected final String name;
    protected final int distance;
    protected final AtomicBoolean closed;

    public NodeSupport(String name, int distance) {
        this.name = requireNonNull(name, "name");
        this.distance = distance;
        this.closed = new AtomicBoolean(false);
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public int distance() {
        return distance;
    }

    @Override
    public final void close() throws IOException {
        if (closed.compareAndSet(false, true)) {
            doClose();
        }
    }

    protected void ensureOpen() {
        if (closed.get()) {
            throw new IllegalStateException("Node already closed");
        }
    }

    protected void doClose() throws IOException {}

    @Override
    public abstract String toString();
}
