/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.shared.impl.file;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

import eu.maveniverse.maven.mimir.shared.Config;
import eu.maveniverse.maven.mimir.shared.impl.SimpleKeyResolverFactory;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

public final class FileNodeConfig {
    public static FileNodeConfig with(Config config) {
        requireNonNull(config, "config");
        String name = NAME;
        int distance = 0;
        Path basedir = config.basedir().resolve(name);
        List<String> checksumAlgorithms = Arrays.asList("SHA-1", "SHA-512");
        String keyResolver = SimpleKeyResolverFactory.NAME;

        if (config.effectiveProperties().containsKey("mimir.file.name")) {
            name = config.effectiveProperties().get("mimir.file.name");
        }
        if (config.effectiveProperties().containsKey("mimir.file.distance")) {
            distance = Integer.parseInt(config.effectiveProperties().get("mimir.file.distance"));
        }
        if (config.effectiveProperties().containsKey("mimir.file.basedir")) {
            basedir =
                    Config.getCanonicalPath(Path.of(config.effectiveProperties().get("mimir.file.basedir")));
        }
        if (config.effectiveProperties().containsKey("mimir.file.checksumAlgorithms")) {
            checksumAlgorithms = Arrays.stream(config.effectiveProperties()
                            .get("mimir.file.checksumAlgorithms")
                            .split(","))
                    .filter(s -> !s.trim().isEmpty())
                    .collect(toList());
        }
        if (config.effectiveProperties().containsKey("mimir.file.keyResolver")) {
            keyResolver = config.effectiveProperties().get("mimir.file.keyResolver");
        }

        return new FileNodeConfig(name, distance, basedir, checksumAlgorithms, keyResolver);
    }

    public static FileNodeConfig of(
            String name, int distance, Path basedir, List<String> checksumAlgorithms, String keyResolver) {
        return new FileNodeConfig(
                requireNonNull(name, "name"),
                distance,
                Config.getCanonicalPath(basedir),
                checksumAlgorithms,
                keyResolver);
    }

    public static final String NAME = "file";

    private final String name;
    private final int distance;
    private final Path basedir;
    private final List<String> checksumAlgorithms;
    private final String keyResolver;

    private FileNodeConfig(
            String name, int distance, Path basedir, List<String> checksumAlgorithms, String keyResolver) {
        this.name = name;
        this.distance = distance;
        this.basedir = basedir;
        this.checksumAlgorithms = List.copyOf(checksumAlgorithms);
        this.keyResolver = keyResolver;
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

    public String keyResolver() {
        return keyResolver;
    }
}
