/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.node.jgroups;

import eu.maveniverse.maven.mimir.shared.Config;
import eu.maveniverse.maven.mimir.shared.node.LocalNode;
import eu.maveniverse.maven.mimir.shared.node.Node;
import eu.maveniverse.maven.mimir.shared.node.NodeFactory;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Optional;
import javax.inject.Named;
import javax.inject.Singleton;
import org.jgroups.JChannel;

@Singleton
@Named(JGroupsNodeFactory.NAME)
public class JGroupsNodeFactory implements NodeFactory {
    public static final String NAME = "jgroups";

    @Override
    public Optional<Node> createNode(Config config, LocalNode localNode) throws IOException {
        try {
            return Optional.of(new JGroupsNode(localNode, createChannel(config)));
        } catch (Exception e) {
            throw new IOException("Failed to create JChannel", e);
        }
    }

    private JChannel createChannel(Config config) throws Exception {
        return new JChannel("udp-new.xml")
                .name(InetAddress.getLocalHost().getHostName())
                .setDiscardOwnMessages(true)
                .connect("mimir-jgroups");
    }
}
