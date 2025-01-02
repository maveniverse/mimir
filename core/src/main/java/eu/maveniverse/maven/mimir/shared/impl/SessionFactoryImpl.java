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
import eu.maveniverse.maven.mimir.shared.naming.NameMapper;
import eu.maveniverse.maven.mimir.shared.node.LocalNode;
import eu.maveniverse.maven.mimir.shared.node.Node;
import eu.maveniverse.maven.mimir.shared.node.NodeFactory;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

@Singleton
@Named
public class SessionFactoryImpl implements SessionFactory {
    private final Map<String, NameMapper> nameMappers;
    private final Map<String, NodeFactory> nodeFactories;

    @Inject
    public SessionFactoryImpl(Map<String, NameMapper> nameMappers, Map<String, NodeFactory> nodeFactories) {
        this.nameMappers = nameMappers;
        this.nodeFactories = nodeFactories;
    }

    @Override
    public Session createSession(Map<String, Object> config) throws IOException {
        LocalNode localNode = createLocalNode(config);
        ArrayList<Node> nodes = new ArrayList<>();
        for (NodeFactory nodeFactory : this.nodeFactories.values()) {
            nodes.add(nodeFactory.createNode(config, localNode));
        }
        nodes.sort(Comparator.comparing(Node::distance));
        return new SessionImpl(nameMappers.get(SimpleNameMapper.NAME), localNode, nodes);
    }

    private LocalNode createLocalNode(Map<String, Object> config) throws IOException {
        Path localBaseDir =
                Paths.get((String) config.get("user.home")).resolve(".mimir").resolve("local");
        Files.createDirectories(localBaseDir);
        return new LocalNodeImpl(localBaseDir);
    }
}
