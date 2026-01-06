/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.shared.impl;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.mimir.shared.Session;
import eu.maveniverse.maven.mimir.shared.SessionConfig;
import eu.maveniverse.maven.mimir.shared.SessionFactory;
import eu.maveniverse.maven.mimir.shared.impl.node.OverlayingLocalNode;
import eu.maveniverse.maven.mimir.shared.mirror.Mirrors;
import eu.maveniverse.maven.mimir.shared.naming.KeyMapperFactory;
import eu.maveniverse.maven.mimir.shared.naming.RemoteRepositories;
import eu.maveniverse.maven.mimir.shared.node.LocalNode;
import eu.maveniverse.maven.mimir.shared.node.LocalNodeFactory;
import eu.maveniverse.maven.shared.core.component.ComponentSupport;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.spi.connector.checksum.ChecksumAlgorithmFactory;

@Singleton
@Named
public final class SessionFactoryImpl extends ComponentSupport implements SessionFactory {
    private final Map<String, LocalNodeFactory<?>> localNodeFactories;
    private final Map<String, KeyMapperFactory> nameMapperFactories;
    private final Map<String, ChecksumAlgorithmFactory> checksumFactories;

    @Inject
    public SessionFactoryImpl(
            Map<String, LocalNodeFactory<?>> localNodeFactories,
            Map<String, KeyMapperFactory> nameMapperFactories,
            Map<String, ChecksumAlgorithmFactory> checksumFactories) {
        this.localNodeFactories = requireNonNull(localNodeFactories, "localNodeFactories");
        this.nameMapperFactories = requireNonNull(nameMapperFactories, "nameMapperFactories");
        this.checksumFactories = requireNonNull(checksumFactories, "checksumFactories");
    }

    @Override
    public Session createSession(SessionConfig config) throws IOException {
        requireNonNull(config);

        KeyMapperFactory keyMapperFactory = nameMapperFactories.get(config.keyMapper());
        if (keyMapperFactory == null) {
            throw new IllegalStateException("No keyMapper: " + config.keyMapper());
        }
        BiFunction<RemoteRepository, Artifact, URI> keyMapper =
                requireNonNull(keyMapperFactory.createKeyMapper(config), "keyMapper");

        ArrayList<LocalNode> overlays = new ArrayList<>();
        if (!config.overlayNodes().isEmpty()) {
            for (String overlay : config.overlayNodes()) {
                createLocalNode(overlay, config).ifPresent(overlays::add);
            }
        }

        LocalNode localNode = config.localNodeInstance().isPresent()
                ? config.localNodeInstance().orElseThrow()
                : createLocalNode(config.localNode(), config)
                        .orElseThrow(() -> new IllegalStateException(
                                "Chosen local node " + config.localNode() + " not configured"));

        if (!overlays.isEmpty()) {
            localNode = new OverlayingLocalNode(overlays, localNode);
        }
        Set<String> repositories = config.repositories();

        if (logger.isDebugEnabled()) {
            logger.debug("Mimir {} session created", config.mimirVersion());
            logger.debug("  Enabled: {}", config.enabled());
            logger.debug("  Basedir: {}", config.basedir());
            logger.debug("  Properties: {}", config.basedir().resolve(config.propertiesPath()));
            logger.debug("  Key mapper: {}", keyMapper.getClass().getSimpleName());
            logger.debug("  Overlays: {}", overlays);
            logger.debug("  Local Node: {}", localNode);
            logger.debug("  Repositories: {}", repositories);
            logger.debug("  Used checksums: {}", localNode.checksumAlgorithms());
            logger.debug("  Supported checksums: {}", checksumFactories.keySet());
        }

        return new SessionImpl(
                config,
                RemoteRepositories.repositoryPredicate(config.repositories()),
                Mirrors.parseMirrors(config, config.mirrors()),
                a -> !a.isSnapshot(),
                keyMapper,
                localNode);
    }

    private Optional<? extends LocalNode> createLocalNode(String name, SessionConfig cfg) throws IOException {
        LocalNodeFactory<? extends LocalNode> localNodeFactory = localNodeFactories.get(name);
        if (localNodeFactory == null) {
            throw new IllegalArgumentException("Unknown local node: " + name);
        }
        return localNodeFactory.createLocalNode(cfg);
    }
}
