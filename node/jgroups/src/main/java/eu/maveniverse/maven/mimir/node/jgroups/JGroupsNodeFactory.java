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
import eu.maveniverse.maven.mimir.shared.node.RemoteNodeFactory;
import eu.maveniverse.maven.mimir.shared.node.SystemNode;
import eu.maveniverse.maven.mimir.shared.publisher.PublisherFactory;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import org.jgroups.JChannel;

@Singleton
@Named(JGroupsNodeConfig.NAME)
public class JGroupsNodeFactory implements RemoteNodeFactory {
    private final SystemNode systemNode;
    private final Map<String, PublisherFactory> publisherFactories;

    @Inject
    public JGroupsNodeFactory(SystemNode systemNode, Map<String, PublisherFactory> publisherFactories) {
        this.systemNode = requireNonNull(systemNode, "systemNode");
        this.publisherFactories = requireNonNull(publisherFactories, "publisherFactories");
    }

    @Override
    public Optional<JGroupsNode> createNode(Config config) throws IOException {
        requireNonNull(config, "config");

        try {
            JGroupsNodeConfig cfg = JGroupsNodeConfig.with(config);
            if (!cfg.enabled()) {
                return Optional.empty();
            }
            if (cfg.publisherEnabled()) {
                PublisherFactory publisherFactory = publisherFactories.get(cfg.publisherTransport());
                if (publisherFactory == null) {
                    throw new IllegalStateException("No publisher found for transport " + cfg.publisherTransport());
                }
                return Optional.of(
                        new JGroupsNode(createChannel(cfg), publisherFactory.createPublisher(config, systemNode)));
            } else {
                return Optional.of(new JGroupsNode(createChannel(cfg)));
            }
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
