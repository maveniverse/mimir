/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.shared.impl;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

import eu.maveniverse.maven.mimir.shared.Config;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

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
            basedir =
                    Config.getCanonicalPath(Path.of(config.effectiveProperties().get("mimir.local.basedir")));
        }
        List<String> checksumAlgorithms = Arrays.asList("SHA-1", "SHA-512");
        if (config.effectiveProperties().containsKey("mimir.local.checksumAlgorithms")) {
            checksumAlgorithms = Arrays.stream(config.effectiveProperties()
                            .get("mimir.local.checksumAlgorithms")
                            .split(","))
                    .filter(s -> !s.trim().isEmpty())
                    .collect(toList());
        }

        return new LocalNodeConfig(name, distance, basedir, checksumAlgorithms);
    }

    public static LocalNodeConfig of(String name, int distance, Path basedir) {
        return new LocalNodeConfig(
                requireNonNull(name, "name"),
                distance,
                Config.getCanonicalPath(basedir),
                Arrays.asList("SHA-1", "SHA-512"));
    }

    public static final String NAME = "local";

    private final String name;
    private final int distance;
    private final Path basedir;
    private final List<String> checksumAlgorithms;

    private LocalNodeConfig(String name, int distance, Path basedir, List<String> checksumAlgorithms) {
        this.name = name;
        this.distance = distance;
        this.basedir = basedir;
        this.checksumAlgorithms = List.copyOf(checksumAlgorithms);
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

    public List<String> checksumAlgorithms() {
        return checksumAlgorithms;
    }
}
