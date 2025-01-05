/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.shared.impl;

import eu.maveniverse.maven.mimir.shared.Session;
import eu.maveniverse.maven.mimir.shared.SessionFactory;
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

@Singleton
@Named
public class SessionFactoryImpl implements SessionFactory {
    private final NameMapperFactory nameMapperFactory;
    private final LocalNodeFactory localNodeFactory;
    private final Map<String, NodeFactory> nodeFactories;

    @Inject
    public SessionFactoryImpl(
            NameMapperFactory nameMapperFactory,
            LocalNodeFactory localNodeFactory,
            Map<String, NodeFactory> nodeFactories) {
        this.nameMapperFactory = nameMapperFactory;
        this.localNodeFactory = localNodeFactory;
        this.nodeFactories = nodeFactories;
    }

    @Override
    public Session createSession(Map<String, Object> config) throws IOException {
        LocalNode localNode = localNodeFactory.createLocalNode(config);
        ArrayList<Node> nodes = new ArrayList<>();
        for (NodeFactory nodeFactory : this.nodeFactories.values()) {
            Optional<Node> node = nodeFactory.createNode(config, localNode);
            node.ifPresent(nodes::add);
        }
        nodes.sort(Comparator.comparing(Node::distance));
        return new SessionImpl(nameMapperFactory.createNameMapper(config), localNode, nodes);
    }
}
