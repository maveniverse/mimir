/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.shared.impl.naming;

import org.eclipse.aether.artifact.Artifact;

/**
 * Helper class to {@link Artifact} instances; provides layout.
 */
public final class Artifacts {
    /**
     * Provides "simple path" for artifact.
     * <p>
     * This was Mimir "old" default mapping function until 0.11.0.
     * @deprecated Should not be used.
     */
    @Deprecated
    public static String artifactSimplePath(Artifact artifact) {
        return artifact.getGroupId() + "/" + artifact.getArtifactId() + "/" + artifact.getBaseVersion() + "/"
                + artifactName(artifact);
    }

    /**
     * Provides "repository path" for artifact.
     */
    public static String artifactRepositoryPath(Artifact artifact) {
        return artifact.getGroupId().replaceAll("\\.", "/") + "/" + artifact.getArtifactId() + "/"
                + artifact.getBaseVersion() + "/" + artifactName(artifact);
    }

    /**
     * Provides "name" for artifact.
     */
    public static String artifactName(Artifact artifact) {
        String name = artifact.getArtifactId() + "-" + artifact.getVersion();
        if (artifact.getClassifier() != null && !artifact.getClassifier().trim().isEmpty()) {
            name += "-" + artifact.getClassifier();
        }
        name += "." + artifact.getExtension();
        return name;
    }
}
