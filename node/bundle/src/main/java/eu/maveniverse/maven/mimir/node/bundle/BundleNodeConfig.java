/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.node.bundle;

import static eu.maveniverse.maven.mimir.shared.impl.ParseUtils.parseBundleSources;
import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.mimir.shared.SessionConfig;
import eu.maveniverse.maven.mimir.shared.impl.ParseUtils;
import java.io.IOException;
import java.util.List;

public class BundleNodeConfig {
    public static BundleNodeConfig with(SessionConfig sessionConfig) throws IOException {
        requireNonNull(sessionConfig, "config");

        List<ParseUtils.ArtifactSource> artifactSources = List.of();
        if (sessionConfig.effectiveProperties().containsKey("mimir.bundle.sources")) {
            artifactSources = parseBundleSources(
                    sessionConfig, sessionConfig.effectiveProperties().get("mimir.bundle.sources"), true);
        }
        return new BundleNodeConfig(artifactSources);
    }

    public static final String NAME = "bundle";

    private final List<ParseUtils.ArtifactSource> artifactSources;

    private BundleNodeConfig(List<ParseUtils.ArtifactSource> artifactSources) {
        this.artifactSources = artifactSources;
    }

    public List<ParseUtils.ArtifactSource> bundleSources() {
        return artifactSources;
    }
}
