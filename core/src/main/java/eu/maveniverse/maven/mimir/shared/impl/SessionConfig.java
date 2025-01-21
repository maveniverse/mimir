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
import eu.maveniverse.maven.mimir.shared.impl.naming.SimpleKeyMapperFactory;

public final class SessionConfig {
    public static SessionConfig with(Config config) {
        requireNonNull(config, "config");

        String keyMapper = SimpleKeyMapperFactory.NAME;
        String localNode = "daemon";

        if (config.effectiveProperties().containsKey("mimir.session.keyMapper")) {
            keyMapper = config.effectiveProperties().get("mimir.session.keyMapper");
        }
        if (config.effectiveProperties().containsKey("mimir.session.localNode")) {
            localNode = config.effectiveProperties().get("mimir.session.localNode");
        }

        return new SessionConfig(keyMapper, localNode);
    }

    public static SessionConfig of(String nameMapper, String localNode) {
        return new SessionConfig(requireNonNull(nameMapper, "nameMapper"), requireNonNull(localNode, "localNode"));
    }

    private final String keyMapper;
    private final String localNode;

    private SessionConfig(String keyMapper, String localNode) {
        this.keyMapper = keyMapper;
        this.localNode = localNode;
    }

    public String keyMapper() {
        return keyMapper;
    }

    public String localNode() {
        return localNode;
    }
}
