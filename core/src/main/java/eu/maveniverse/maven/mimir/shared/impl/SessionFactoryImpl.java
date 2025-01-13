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
import eu.maveniverse.maven.mimir.shared.naming.NameMapper;
import eu.maveniverse.maven.mimir.shared.naming.NameMapperFactory;
import eu.maveniverse.maven.mimir.shared.naming.RemoteRepositories;
import eu.maveniverse.maven.mimir.shared.node.LocalNode;
import eu.maveniverse.maven.mimir.shared.node.LocalNodeFactory;
import eu.maveniverse.maven.mimir.shared.node.Node;
import eu.maveniverse.maven.mimir.shared.node.NodeFactory;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import org.eclipse.aether.spi.connector.checksum.ChecksumAlgorithmFactory;
import org.eclipse.aether.spi.connector.checksum.ChecksumAlgorithmFactorySelector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@Named
public class SessionFactoryImpl implements SessionFactory {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final ChecksumAlgorithmFactorySelector checksumAlgorithmFactorySelector;
    private final LocalNodeFactory localNodeFactory;
    private final Map<String, NodeFactory> nodeFactories;
    private final Map<String, NameMapperFactory> nameMapperFactories;

    @Inject
    public SessionFactoryImpl(
            ChecksumAlgorithmFactorySelector checksumAlgorithmFactorySelector,
            LocalNodeFactory localNodeFactory,
            Map<String, NodeFactory> nodeFactories,
            Map<String, NameMapperFactory> nameMapperFactories) {
        this.checksumAlgorithmFactorySelector =
                requireNonNull(checksumAlgorithmFactorySelector, "checksumAlgorithmFactorySelector");
        this.localNodeFactory = requireNonNull(localNodeFactory, "localNodeFactory");
        this.nodeFactories = requireNonNull(nodeFactories, "nodeFactories");
        this.nameMapperFactories = requireNonNull(nameMapperFactories, "nameMapperFactories");
    }

    @Override
    public Session createSession(Config config) throws IOException {
        requireNonNull(config, "config");
        LocalNode localNode = localNodeFactory.createLocalNode(config);
        ArrayList<Node> nodes = new ArrayList<>();
        for (NodeFactory nodeFactory : this.nodeFactories.values()) {
            Optional<Node> node = nodeFactory.createNode(config, localNode);
            node.ifPresent(nodes::add);
        }
        nodes.sort(Comparator.comparing(Node::distance));
        NameMapper nameMapper = null;
        for (NameMapperFactory nameMapperFactory : this.nameMapperFactories.values()) {
            Optional<NameMapper> optional = nameMapperFactory.createNameMapper(config);
            if (optional.isPresent()) {
                nameMapper = optional.orElseThrow();
                break;
            }
        }
        if (nameMapper == null) {
            throw new IllegalStateException("No nameMapper found");
        }
        logger.info("Mimir {} session created", config.mimirVersion());
        if (logger.isDebugEnabled()) {
            logger.debug("  Name mapper: {}", nameMapper.getClass().getSimpleName());
            logger.debug("  Local Node: {}", localNode);
            logger.debug("  {} node(s):", nodes.size());
            for (Node node : nodes) {
                logger.debug("    {} (d={})", node.name(), node.distance());
            }
        }
        Map<String, ChecksumAlgorithmFactory> factories = new HashMap<>();

        return new SessionImpl(
                factories, RemoteRepositories.centralDirectOnly(), a -> !a.isSnapshot(), nameMapper, localNode, nodes);
    }
}
