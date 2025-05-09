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
import eu.maveniverse.maven.mimir.shared.naming.RemoteRepositories;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class SessionConfig {
    public static SessionConfig with(Config config) {
        requireNonNull(config, "config");

        String keyMapper = SimpleKeyMapperFactory.NAME;
        String localNode = "daemon";
        Set<String> repositories = Collections.singleton(RemoteRepositories.CENTRAL_REPOSITORY_ID);

        if (config.effectiveProperties().containsKey("mimir.session.keyMapper")) {
            keyMapper = config.effectiveProperties().get("mimir.session.keyMapper");
        }
        if (config.effectiveProperties().containsKey("mimir.session.localNode")) {
            localNode = config.effectiveProperties().get("mimir.session.localNode");
        }
        if (config.effectiveProperties().containsKey("mimir.session.repositories")) {
            String value = config.effectiveProperties().get("mimir.session.repositories");
            repositories = new HashSet<>(Arrays.asList(value.split(",")));
        }

        return new SessionConfig(keyMapper, localNode, repositories);
    }

    private final String keyMapper;
    private final String localNode;
    private final Set<String> repositories;

    private SessionConfig(String keyMapper, String localNode, Set<String> repositories) {
        this.keyMapper = keyMapper;
        this.localNode = localNode;
        this.repositories = repositories;
    }

    public String keyMapper() {
        return keyMapper;
    }

    public String localNode() {
        return localNode;
    }

    public Set<String> repositories() {
        return repositories;
    }
}
