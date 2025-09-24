/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.node.jgroups;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.mimir.shared.SessionConfig;
import eu.maveniverse.maven.mimir.shared.node.RemoteNodeFactory;
import eu.maveniverse.maven.mimir.shared.node.SystemNode;
import eu.maveniverse.maven.mimir.shared.publisher.PublisherFactory;
import eu.maveniverse.maven.shared.core.component.ComponentSupport;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import org.jgroups.JChannel;

@Singleton
@Named(JGroupsNodeConfig.NAME)
public class JGroupsNodeFactory extends ComponentSupport implements RemoteNodeFactory<JGroupsNode> {
    private final SystemNode systemNode;
    private final Map<String, PublisherFactory> publisherFactories;

    @Inject
    public JGroupsNodeFactory(SystemNode systemNode, Map<String, PublisherFactory> publisherFactories) {
        this.systemNode = requireNonNull(systemNode, "systemNode");
        this.publisherFactories = requireNonNull(publisherFactories, "publisherFactories");
    }

    @Override
    public Optional<JGroupsNode> createRemoteNode(SessionConfig sessionConfig) throws IOException {
        requireNonNull(sessionConfig, "config");

        try {
            JGroupsNodeConfig cfg = JGroupsNodeConfig.with(sessionConfig);
            if (!cfg.enabled()) {
                logger.info("JGroupsNode is disabled");
                return Optional.empty();
            }
            if (cfg.publisherEnabled()) {
                PublisherFactory publisherFactory = publisherFactories.get(cfg.publisherTransport());
                if (publisherFactory == null) {
                    throw new IllegalStateException("No publisher found for transport " + cfg.publisherTransport());
                }
                return Optional.of(new JGroupsNode(
                        cfg.jgroupsClusterName(),
                        createChannel(sessionConfig, cfg),
                        publisherFactory.createPublisher(sessionConfig, systemNode)));
            } else {
                return Optional.of(new JGroupsNode(cfg.jgroupsClusterName(), createChannel(sessionConfig, cfg)));
            }
        } catch (Exception e) {
            throw new IOException("Failed to create JChannel", e);
        }
    }

    private JChannel createChannel(SessionConfig sessionConfig, JGroupsNodeConfig cfg) throws Exception {
        if (System.getProperty("jgroups.bind_addr") == null) {
            String hint = null;
            if (cfg.jgroupsInterface() != null) {
                hint = cfg.jgroupsInterface();
            } else if (sessionConfig.localHostHint().isPresent()) {
                hint = sessionConfig.localHostHint().orElseThrow();
            }
            if (hint != null) {
                // hack, find better way
                System.setProperty("jgroups.bind_addr", hint);
            }
        }
        return new JChannel(cfg.jgroupsProps()).name(cfg.jgroupsNodeName()).setDiscardOwnMessages(true);
    }
}
