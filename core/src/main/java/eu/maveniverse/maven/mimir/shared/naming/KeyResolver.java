/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.shared.naming;

import java.net.URI;
import java.util.function.Function;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;

public interface KeyResolver extends Function<URI, Key> {
    /**
     * Provides "path" for artifact.
     */
    default String artifactPath(Artifact artifact) {
        String name = artifact.getGroupId() + "/" + artifact.getArtifactId() + "/" + artifact.getBaseVersion() + "/"
                + artifact.getArtifactId() + "-" + artifact.getVersion();
        if (artifact.getClassifier() != null && !artifact.getClassifier().trim().isEmpty()) {
            name += "-" + artifact.getClassifier();
        }
        name += "." + artifact.getExtension();
        return name;
    }

    /**
     * Resolves a cache key according to naming strategy.
     */
    @Override
    default Key apply(URI key) {
        if (key.isOpaque()) {
            if ("mimir".equals(key.getScheme())) {
                String ssp = key.getSchemeSpecificPart();
                if (ssp.startsWith("artifact:")) {
                    String[] bits = ssp.substring(9).split(":", 2);
                    if (bits.length == 2) {
                        String container = bits[0];
                        Artifact artifact = new DefaultArtifact(bits[1]);
                        return Key.of(container, artifactPath(artifact));
                    }
                } else if (ssp.startsWith("file:")) {
                    String[] bits = ssp.substring(5).split(":", 2);
                    if (bits.length == 2) {
                        String container = bits[0];
                        String path = bits[1];
                        return Key.of(container, path);
                    }
                }
            }
        }
        throw new IllegalArgumentException("Unexpected URI");
    }
}
