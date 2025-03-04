/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.shared;

import eu.maveniverse.maven.mimir.shared.node.LocalEntry;
import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.repository.RemoteRepository;

public interface Session extends Closeable {
    /**
     * Tells whether session is configured to support given remote repository.
     */
    boolean repositorySupported(RemoteRepository remoteRepository);

    /**
     * Tells whether session is configured to support given artifact.
     */
    boolean artifactSupported(Artifact artifact);

    /**
     * Provides list of checksum algorithm names configured to be used by this node.
     */
    List<String> checksumAlgorithms() throws IOException;

    /**
     * Locates cache entry by key.
     */
    Optional<LocalEntry> locate(RemoteRepository remoteRepository, Artifact artifact) throws IOException;

    /**
     * Stores entry under given cache key.
     */
    Optional<LocalEntry> store(
            RemoteRepository remoteRepository,
            Artifact artifact,
            Path file,
            Map<String, String> metadata,
            Map<String, String> checksums)
            throws IOException;
}
