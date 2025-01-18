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
import eu.maveniverse.maven.mimir.shared.impl.file.FileNodeConfig;

public final class SessionConfig {
    public static SessionConfig with(Config config) {
        requireNonNull(config, "config");

        String keyMapper = SimpleKeyMapperFactory.NAME;
        String localNodeName = FileNodeConfig.NAME;

        if (config.effectiveProperties().containsKey("mimir.session.keyMapper")) {
            keyMapper = config.effectiveProperties().get("mimir.session.keyMapper");
        }
        if (config.effectiveProperties().containsKey("mimir.session.localNodeName")) {
            localNodeName = config.effectiveProperties().get("mimir.session.localNodeName");
        }

        return new SessionConfig(keyMapper, localNodeName);
    }

    public static SessionConfig of(String nameMapper, String localNodeName) {
        return new SessionConfig(
                requireNonNull(nameMapper, "nameMapper"), requireNonNull(localNodeName, "localNodeName"));
    }

    private final String keyMapper;
    private final String localNodeName;

    private SessionConfig(String keyMapper, String localNodeName) {
        this.keyMapper = keyMapper;
        this.localNodeName = localNodeName;
    }

    public String keyMapper() {
        return keyMapper;
    }

    public String localNodeName() {
        return localNodeName;
    }
}
