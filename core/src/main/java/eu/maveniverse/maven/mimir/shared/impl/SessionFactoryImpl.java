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
import eu.maveniverse.maven.mimir.shared.Session;
import eu.maveniverse.maven.mimir.shared.SessionFactory;
import eu.maveniverse.maven.mimir.shared.naming.KeyMapperFactory;
import eu.maveniverse.maven.mimir.shared.naming.RemoteRepositories;
import eu.maveniverse.maven.mimir.shared.node.LocalNode;
import eu.maveniverse.maven.mimir.shared.node.LocalNodeFactory;
import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.function.BiFunction;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@Named
public final class SessionFactoryImpl implements SessionFactory {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final Map<String, LocalNodeFactory> localNodeFactories;
    private final Map<String, KeyMapperFactory> nameMapperFactories;

    @Inject
    public SessionFactoryImpl(
            Map<String, LocalNodeFactory> localNodeFactories, Map<String, KeyMapperFactory> nameMapperFactories) {
        this.localNodeFactories = requireNonNull(localNodeFactories, "localNodeFactories");
        this.nameMapperFactories = requireNonNull(nameMapperFactories, "nameMapperFactories");
    }

    @Override
    public Session createSession(Config config) throws IOException {
        requireNonNull(config, "config");

        SessionConfig sessionConfig = SessionConfig.with(config);

        LocalNodeFactory localNodeFactory = localNodeFactories.get(sessionConfig.localNode());
        if (localNodeFactory == null) {
            throw new IllegalArgumentException("Unknown local node: " + sessionConfig.localNode());
        }
        LocalNode<?> localNode = localNodeFactory.createNode(config);

        KeyMapperFactory keyMapperFactory = nameMapperFactories.get(sessionConfig.keyMapper());
        if (keyMapperFactory == null) {
            throw new IllegalStateException("No keyMapper: " + sessionConfig.keyMapper());
        }
        BiFunction<RemoteRepository, Artifact, URI> nameMapper =
                requireNonNull(keyMapperFactory.createKeyMapper(config), "keyMapper");

        if (logger.isDebugEnabled()) {
            logger.debug("Mimir {} session created", config.mimirVersion().orElse("UNKNOWN"));
            logger.debug("  Properties: {}", config.basedir().resolve(config.propertiesPath()));
            logger.debug("  Name mapper: {}", nameMapper.getClass().getSimpleName());
            logger.debug("  Local Node: {}", localNode);
            logger.debug("  Used checksums: {}", localNode.checksumAlgorithms());
            logger.debug(
                    "  Supported checksums: {}", localNode.checksumFactories().keySet());
        }

        return new SessionImpl(RemoteRepositories.centralDirectOnly(), a -> !a.isSnapshot(), nameMapper, localNode);
    }
}
