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

        List<ParseUtils.BundleSource> bundleSources = List.of();
        if (sessionConfig.effectiveProperties().containsKey("mimir.bundle.sources")) {
            bundleSources = parseBundleSources(
                    sessionConfig, sessionConfig.effectiveProperties().get("mimir.bundle.sources"));
        }
        return new BundleNodeConfig(bundleSources);
    }

    public static final String NAME = "bundle";

    private final List<ParseUtils.BundleSource> bundleSources;

    private BundleNodeConfig(List<ParseUtils.BundleSource> bundleSources) {
        this.bundleSources = bundleSources;
    }

    public List<ParseUtils.BundleSource> bundleSources() {
        return bundleSources;
    }
}
