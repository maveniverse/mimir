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
import eu.maveniverse.maven.mimir.shared.node.Node;
import eu.maveniverse.maven.mimir.shared.node.RemoteNode;
import eu.maveniverse.maven.mimir.shared.node.RemoteNodeFactory;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.spi.connector.checksum.ChecksumAlgorithmFactory;
import org.eclipse.aether.spi.connector.checksum.ChecksumAlgorithmFactorySelector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@Named
public final class SessionFactoryImpl implements SessionFactory {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final LocalNodeFactory localNodeFactory;
    private final Map<String, RemoteNodeFactory> remoteNodeFactories;
    private final Map<String, KeyMapperFactory> nameMapperFactories;

    @Inject
    public SessionFactoryImpl(
            ChecksumAlgorithmFactorySelector checksumAlgorithmFactorySelector,
            LocalNodeFactory localNodeFactory,
            Map<String, RemoteNodeFactory> remoteNodeFactories,
            Map<String, KeyMapperFactory> nameMapperFactories) {
        this.localNodeFactory = requireNonNull(localNodeFactory, "localNodeFactory");
        this.remoteNodeFactories = requireNonNull(remoteNodeFactories, "remoteNodeFactories");
        this.nameMapperFactories = requireNonNull(nameMapperFactories, "nameMapperFactories");
    }

    @Override
    public Session createSession(Config config) throws IOException {
        requireNonNull(config, "config");
        LocalNode localNode = localNodeFactory.createLocalNode(config);
        Map<String, ChecksumAlgorithmFactory> factories = localNode.checksumFactories();

        ArrayList<RemoteNode> remoteNodes = new ArrayList<>();
        for (RemoteNodeFactory nodeFactory : this.remoteNodeFactories.values()) {
            Optional<RemoteNode> node = nodeFactory.createNode(config);
            node.ifPresent(remoteNodes::add);
        }
        remoteNodes.sort(Comparator.comparing(Node::distance));

        SessionConfig sessionConfig = SessionConfig.with(config);
        KeyMapperFactory keyMapperFactory = nameMapperFactories.get(sessionConfig.keyMapper());
        if (keyMapperFactory == null) {
            throw new IllegalStateException("No keyMapper: " + sessionConfig.keyMapper());
        }
        BiFunction<RemoteRepository, Artifact, URI> nameMapper =
                requireNonNull(keyMapperFactory.createKeyMapper(config), "keyMapper");

        if (logger.isDebugEnabled()) {
            logger.debug("Mimir {} session created", config.mimirVersion().orElse("UNKNOWN"));
            logger.debug("  Name mapper: {}", nameMapper.getClass().getSimpleName());
            logger.debug("  Checksums: {}", factories.keySet());
            logger.debug("  Local Node: {}", localNode);
            logger.debug("  {} remote node(s):", remoteNodes.size());
            for (RemoteNode node : remoteNodes) {
                logger.debug("    {} (d={})", node.name(), node.distance());
            }
        }

        return new SessionImpl(
                factories,
                RemoteRepositories.centralDirectOnly(),
                a -> !a.isSnapshot(),
                nameMapper,
                localNode,
                remoteNodes);
    }
}
