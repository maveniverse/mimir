/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.shared.naming;

import java.net.URI;
import java.util.function.BiFunction;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.util.artifact.ArtifactIdUtils;

public interface KeyMapper extends BiFunction<RemoteRepository, Artifact, URI> {
    /**
     * Provides default and simplistic "container" implementation.
     */
    default String container(RemoteRepository repository) {
        return repository.getId();
    }

    /**
     * Provides default and simplistic "name" implementation.
     */
    default String name(Artifact artifact) {
        return ArtifactIdUtils.toId(artifact);
    }

    /**
     * Creates a cache key according to naming strategy.
     */
    @Override
    default URI apply(RemoteRepository remoteRepository, Artifact artifact) {
        return URI.create("mimir:artifact:" + container(remoteRepository) + ":" + name(artifact));
    }
}
