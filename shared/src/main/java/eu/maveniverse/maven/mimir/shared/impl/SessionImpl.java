/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.shared.impl;

import eu.maveniverse.maven.mimir.shared.CacheEntry;
import eu.maveniverse.maven.mimir.shared.CacheKey;
import eu.maveniverse.maven.mimir.shared.Session;
import eu.maveniverse.maven.mimir.shared.node.Node;
import eu.maveniverse.maven.mimir.shared.node.WritableNode;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SessionImpl implements Session {
    private final List<Node> nodes;

    public SessionImpl(List<Node> nodes) {
        this.nodes = nodes;
    }

    @Override
    public Optional<CacheEntry> locate(CacheKey key) throws IOException {
        Optional<CacheEntry> result;
        for (Node node : this.nodes) {
            result = node.locate(key);
            if (result.isPresent()) {
                return result;
            }
        }
        return Optional.empty();
    }

    @Override
    public boolean store(CacheKey key, Path content) throws IOException {
        for (Node node : this.nodes) {
            if (node instanceof WritableNode) {
                return ((WritableNode) node).store(key, content);
            }
        }
        return false;
    }

    @Override
    public void close() {
        ArrayList<Exception> exceptions = new ArrayList<>();
        for (Node node : this.nodes) {
            try {
                node.close();
            } catch (Exception e) {
                exceptions.add(e);
            }
        }
        if (!exceptions.isEmpty()) {
            IllegalStateException illegalStateException = new IllegalStateException("Could not close session");
            exceptions.forEach(illegalStateException::addSuppressed);
            throw illegalStateException;
        }
    }
}
