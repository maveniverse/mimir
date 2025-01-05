/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.shared.impl;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.mimir.shared.Config;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class LocalNodeConfig {
    public static LocalNodeConfig with(Config config) {
        requireNonNull(config, "config");
        String name = NAME;
        if (config.effectiveProperties().containsKey("mimir.local.name")) {
            name = config.effectiveProperties().get("mimir.local.name");
        }
        int distance = 0;
        if (config.effectiveProperties().containsKey("mimir.local.distance")) {
            distance = Integer.parseInt(config.effectiveProperties().get("mimir.local.distance"));
        }
        Path basedir = config.basedir().resolve(name);
        if (config.effectiveProperties().containsKey("mimir.local.basedir")) {
            basedir = Config.getCanonicalPath(
                    Paths.get(config.effectiveProperties().get("mimir.local.basedir")));
        }
        return new LocalNodeConfig(name, distance, basedir);
    }

    public static LocalNodeConfig of(String name, int distance, Path basedir) {
        return new LocalNodeConfig(requireNonNull(name, "name"), distance, Config.getCanonicalPath(basedir));
    }

    public static final String NAME = "local";

    private final String name;
    private final int distance;
    private final Path basedir;

    private LocalNodeConfig(String name, int distance, Path basedir) {
        this.name = name;
        this.distance = distance;
        this.basedir = basedir;
    }

    public String name() {
        return name;
    }

    public int distance() {
        return distance;
    }

    public Path basedir() {
        return basedir;
    }
}
