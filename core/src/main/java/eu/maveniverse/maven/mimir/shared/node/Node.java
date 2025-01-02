/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.shared.node;

import eu.maveniverse.maven.mimir.shared.CacheEntry;
import eu.maveniverse.maven.mimir.shared.CacheKey;
import java.io.IOException;
import java.util.Optional;

public interface Node extends AutoCloseable {
    /**
     * Node ID.
     */
    String id();

    /**
     * The "distance" of node. Distance of 0 (zero) is "closest", and positive integers are "further away".
     */
    int distance();

    /**
     * Locates cache entry by key on this node.
     */
    Optional<CacheEntry> locate(CacheKey key) throws IOException;
}
