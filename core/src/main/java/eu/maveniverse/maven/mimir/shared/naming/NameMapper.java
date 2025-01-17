/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.shared.naming;

import eu.maveniverse.maven.mimir.shared.node.Key;
import java.util.function.BiFunction;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.repository.RemoteRepository;

public interface NameMapper extends BiFunction<RemoteRepository, Artifact, Key> {
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
        String name = artifact.getGroupId() + "/" + artifact.getArtifactId() + "/" + artifact.getBaseVersion() + "/"
                + artifact.getArtifactId() + "-" + artifact.getVersion();
        if (artifact.getClassifier() != null && !artifact.getClassifier().trim().isEmpty()) {
            name += "-" + artifact.getClassifier();
        }
        name += "." + artifact.getExtension();
        return name;
    }

    /**
     * Creates a cache key according to naming strategy, if supported. If no cache key creation possible, Mimir
     * will step aside for given transaction.
     */
    @Override
    default Key apply(RemoteRepository remoteRepository, Artifact artifact) {
        return Key.of(container(remoteRepository), name(artifact));
    }
}
