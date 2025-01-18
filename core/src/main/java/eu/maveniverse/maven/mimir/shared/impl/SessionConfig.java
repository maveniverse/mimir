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

public final class SessionConfig {
    public static SessionConfig with(Config config) {
        requireNonNull(config, "config");
        String keyMapper = SimpleKeyMapperFactory.NAME;
        if (config.effectiveProperties().containsKey("mimir.session.keyMapper")) {
            keyMapper = config.effectiveProperties().get("mimir.session.keyMapper");
        }

        return new SessionConfig(keyMapper);
    }

    public static SessionConfig of(String nameMapper) {
        return new SessionConfig(requireNonNull(nameMapper, "nameMapper"));
    }

    private final String keyMapper;

    private SessionConfig(String keyMapper) {
        this.keyMapper = keyMapper;
    }

    public String keyMapper() {
        return keyMapper;
    }
}
