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
import eu.maveniverse.maven.mimir.shared.node.LocalNode;
import eu.maveniverse.maven.mimir.shared.node.LocalNodeFactory;
import eu.maveniverse.maven.mimir.shared.node.Node;
import eu.maveniverse.maven.mimir.shared.node.NodeFactory;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@Named
public class SessionFactoryImpl implements SessionFactory {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final LocalNodeFactory localNodeFactory;
    private final Map<String, NodeFactory> nodeFactories;
    private final Map<String, NameMapperFactory> nameMapperFactories;

    @Inject
    public SessionFactoryImpl(
            LocalNodeFactory localNodeFactory,
            Map<String, NodeFactory> nodeFactories,
            Map<String, NameMapperFactory> nameMapperFactories) {
        this.localNodeFactory = localNodeFactory;
        this.nodeFactories = nodeFactories;
        this.nameMapperFactories = nameMapperFactories;
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
        NameMapper nameMapper = NameMapper.NOP;
        for (NameMapperFactory nameMapperFactory : this.nameMapperFactories.values()) {
            Optional<NameMapper> optional = nameMapperFactory.createNameMapper(config);
            if (optional.isPresent()) {
                nameMapper = optional.orElseThrow();
                break;
            }
        }
        logger.info("Mimir {} session created", config.mimirVersion());
        if (logger.isDebugEnabled()) {
            logger.debug(
                    "  Name mapper: {}",
                    nameMapper == NameMapper.NOP ? "NOP" : nameMapper.getClass().getSimpleName());
            logger.debug(
                    "  Local Node: {} (basedir={}, d={})", localNode.name(), localNode.basedir(), localNode.distance());
            logger.debug("  {} node(s):", nodes.size());
            for (Node node : nodes) {
                logger.debug("    {} (d={})", node.name(), node.distance());
            }
        }
        return new SessionImpl(nameMapper, localNode, nodes);
    }
}
