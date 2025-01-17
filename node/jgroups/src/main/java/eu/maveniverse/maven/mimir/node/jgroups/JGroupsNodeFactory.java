/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.node.jgroups;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.mimir.shared.Config;
import eu.maveniverse.maven.mimir.shared.node.LocalNodeFactory;
import eu.maveniverse.maven.mimir.shared.node.Node;
import eu.maveniverse.maven.mimir.shared.node.NodeFactory;
import java.io.IOException;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import org.jgroups.JChannel;

@Singleton
@Named(JGroupsNodeFactory.NAME)
public class JGroupsNodeFactory implements NodeFactory {
    public static final String NAME = "jgroups";

    private final LocalNodeFactory localNodeFactory;

    @Inject
    public JGroupsNodeFactory(LocalNodeFactory localNodeFactory) {
        this.localNodeFactory = requireNonNull(localNodeFactory, "localNodeFactory");
    }

    @Override
    public Optional<Node> createNode(Config config) throws IOException {
        try {
            JGroupsNodeConfig cfg = JGroupsNodeConfig.with(config);
            if (!cfg.enabled()) {
                return Optional.empty();
            }
            return Optional.of(new JGroupsNode(
                    localNodeFactory.createLocalNode(config), createChannel(cfg), cfg.publisherEnabled()));
        } catch (Exception e) {
            throw new IOException("Failed to create JChannel", e);
        }
    }

    private JChannel createChannel(JGroupsNodeConfig cfg) throws Exception {
        if (cfg.jgroupsInterface() != null && System.getProperty("jgroups.bind_addr") == null) {
            // hack, find better way
            System.setProperty("jgroups.bind_addr", cfg.jgroupsInterface());
        }
        return new JChannel(cfg.jgroupsProps())
                .name(cfg.jgroupsNodeName())
                .setDiscardOwnMessages(true)
                .connect(cfg.jgroupsClusterName());
    }
}
