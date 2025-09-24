/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.shared.impl;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.mimir.shared.SessionConfig;
import eu.maveniverse.maven.mimir.shared.impl.naming.SimpleKeyMapperFactory;
import eu.maveniverse.maven.mimir.shared.naming.RemoteRepositories;
import java.util.Arrays;
import java.util.Set;

public final class SessionImplConfig {
    public static SessionImplConfig with(SessionConfig sessionConfig) {
        requireNonNull(sessionConfig);

        String keyMapper = SimpleKeyMapperFactory.NAME;
        Set<String> overlayNodes = Set.of();
        String localNode = "daemon";
        Set<String> repositories = Set.of(RemoteRepositories.CENTRAL_REPOSITORY_ID);

        if (sessionConfig.effectiveProperties().containsKey("mimir.session.keyMapper")) {
            keyMapper = sessionConfig.effectiveProperties().get("mimir.session.keyMapper");
        }
        if (sessionConfig.effectiveProperties().containsKey("mimir.session.overlayNodes")) {
            overlayNodes = Set.copyOf(Arrays.asList(sessionConfig
                    .effectiveProperties()
                    .get("mimir.session.overlayNodes")
                    .split(",")));
        }
        if (sessionConfig.effectiveProperties().containsKey("mimir.session.localNode")) {
            localNode = sessionConfig.effectiveProperties().get("mimir.session.localNode");
        }
        if (sessionConfig.effectiveProperties().containsKey("mimir.session.repositories")) {
            String value = sessionConfig.effectiveProperties().get("mimir.session.repositories");
            repositories = Set.copyOf(Arrays.asList(value.split(",")));
        }

        return new SessionImplConfig(keyMapper, overlayNodes, localNode, repositories);
    }

    private final String keyMapper;
    private final Set<String> overlayNodes;
    private final String localNode;
    private final Set<String> repositories;

    private SessionImplConfig(String keyMapper, Set<String> overlayNodes, String localNode, Set<String> repositories) {
        this.keyMapper = requireNonNull(keyMapper);
        this.overlayNodes = requireNonNull(overlayNodes);
        this.localNode = requireNonNull(localNode);
        this.repositories = requireNonNull(repositories);
    }

    public String keyMapper() {
        return keyMapper;
    }

    public Set<String> overlayNodes() {
        return overlayNodes;
    }

    public String localNode() {
        return localNode;
    }

    public Set<String> repositories() {
        return repositories;
    }
}
