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
import java.util.Arrays;
import java.util.List;

public final class SessionConfig {
    public static SessionConfig with(Config config) {
        requireNonNull(config, "config");
        String nameMapper = SimpleNameMapperFactory.NAME;
        if (config.effectiveProperties().containsKey("mimir.session.nameMapper")) {
            nameMapper = config.effectiveProperties().get("mimir.session.nameMapper");
        }
        List<String> checksumAlgorithms = Arrays.asList("SHA-1", "SHA-512");
        if (config.effectiveProperties().containsKey("mimir.session.checksumAlgorithms")) {
            checksumAlgorithms = Arrays.stream(config.effectiveProperties()
                            .get("mimir.session.checksumAlgorithms")
                            .split(","))
                    .filter(s -> !s.trim().isEmpty())
                    .collect(toList());
        }

        return new SessionConfig(nameMapper, checksumAlgorithms);
    }

    public static SessionConfig of(String nameMapper, List<String> checksumAlgorithms) {
        return new SessionConfig(requireNonNull(nameMapper, "nameMapper"), checksumAlgorithms);
    }

    private final String nameMapper;
    private final List<String> checksumAlgorithms;

    private SessionConfig(String nameMapper, List<String> checksumAlgorithms) {
        this.nameMapper = nameMapper;
        this.checksumAlgorithms = List.copyOf(checksumAlgorithms);
    }

    public String nameMapper() {
        return nameMapper;
    }

    public List<String> checksumAlgorithms() {
        return checksumAlgorithms;
    }
}
