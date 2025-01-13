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
import java.util.Map;
import java.util.Optional;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.spi.connector.checksum.ChecksumAlgorithmFactory;

public interface Session extends AutoCloseable {
    /**
     * Tells whether session is configured to support given remote repository.
     */
    boolean repositorySupported(RemoteRepository remoteRepository);

    /**
     * Tells session configured checksum factories.
     */
    Map<String, ChecksumAlgorithmFactory> checksumFactories();

    /**
     * Tells whether session is configured to support given remote repository and artifact coming from it.
     */
    boolean artifactSupported(RemoteRepository remoteRepository, Artifact artifact);

    /**
     * Locates cache entry by key.
     */
    Optional<CacheEntry> locate(RemoteRepository remoteRepository, Artifact artifact) throws IOException;

    /**
     * Stores entry under given cache key.
     */
    boolean store(RemoteRepository remoteRepository, Artifact artifact, Path file, Map<String, String> checksums)
            throws IOException;
}
