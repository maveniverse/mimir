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
import java.nio.file.Path;

public interface LocalNode extends Node {
    /**
     * Tells the basedir local node uses.
     */
    Path basedir();

    /**
     * Stores content under given cache key on this node.
     */
    LocalCacheEntry store(CacheKey key, Path content) throws IOException;

    /**
     * Stores cache entry and offers it as own entry.
     */
    LocalCacheEntry store(CacheKey key, CacheEntry entry) throws IOException;
}
