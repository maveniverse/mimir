/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.shared;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.repository.RemoteRepository;

public interface Session extends AutoCloseable {
    /**
     * Tells whether session is configured to support given remote repository.
     */
    boolean supports(RemoteRepository remoteRepository);

    /**
     * Creates cache key out of remote repository and artifact, if supported.
     */
    Optional<CacheKey> cacheKey(RemoteRepository remoteRepository, Artifact artifact);

    /**
     * Locates cache entry by key.
     */
    Optional<CacheEntry> locate(CacheKey key) throws IOException;

    /**
     * Stores content under given cache key.
     */
    void store(CacheKey key, Path content) throws IOException;
}
