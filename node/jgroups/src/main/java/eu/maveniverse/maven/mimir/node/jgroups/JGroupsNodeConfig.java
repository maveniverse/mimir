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
import java.io.IOException;
import java.net.InetAddress;

public class JGroupsNodeConfig {
    public static JGroupsNodeConfig with(Config config) throws IOException {
        requireNonNull(config, "config");

        String groupSuffix = "@n/a";
        if (config.mimirVersion().isPresent()) {
            String version = config.mimirVersion().orElseThrow();
            groupSuffix = "@" + version.substring(0, version.lastIndexOf('.'));
        }

        boolean enabled = true;
        boolean publisherEnabled = true;
        String publisherTransport = "socket";
        String jgroupsProps = "udp-new.xml";
        String jgroupsNodeName = InetAddress.getLocalHost().getHostName();
        String jgroupsClusterName = "mimir-jgroups" + groupSuffix;
        String jgroupsInterface = null;

        if (config.effectiveProperties().containsKey("mimir.jgroups.enabled")) {
            enabled = Boolean.parseBoolean(config.effectiveProperties().get("mimir.jgroups.enabled"));
        }
        if (config.effectiveProperties().containsKey("mimir.jgroups.publisher.enabled")) {
            publisherEnabled =
                    Boolean.parseBoolean(config.effectiveProperties().get("mimir.jgroups.publisher.enabled"));
        }
        if (config.effectiveProperties().containsKey("mimir.jgroups.publisher.transport")) {
            publisherTransport = config.effectiveProperties().get("mimir.jgroups.publisher.transport");
        }
        if (config.effectiveProperties().containsKey("mimir.jgroups.props")) {
            jgroupsProps = config.effectiveProperties().get("mimir.jgroups.props");
        }
        if (config.effectiveProperties().containsKey("mimir.jgroups.nodeName")) {
            jgroupsNodeName = config.effectiveProperties().get("mimir.jgroups.nodeName");
        }
        if (config.effectiveProperties().containsKey("mimir.jgroups.clusterName")) {
            jgroupsClusterName = config.effectiveProperties().get("mimir.jgroups.clusterName");
        }
        if (config.effectiveProperties().containsKey("mimir.jgroups.interface")) {
            jgroupsInterface = config.effectiveProperties().get("mimir.jgroups.interface");
        }
        return new JGroupsNodeConfig(
                enabled,
                publisherEnabled,
                publisherTransport,
                jgroupsProps,
                jgroupsNodeName,
                jgroupsClusterName,
                jgroupsInterface);
    }

    public static final String NAME = "jgroups";

    private final boolean enabled;
    private final boolean publisherEnabled;
    private final String publisherTransport;
    private final String jgroupsProps;
    private final String jgroupsNodeName;
    private final String jgroupsClusterName;
    private final String jgroupsInterface;

    private JGroupsNodeConfig(
            boolean enabled,
            boolean publisherEnabled,
            String publisherTransport,
            String jgroupsProps,
            String jgroupsNodeName,
            String jgroupsClusterName,
            String jgroupsInterface) {
        this.enabled = enabled;
        this.publisherEnabled = publisherEnabled;
        this.publisherTransport = publisherTransport;
        this.jgroupsProps = jgroupsProps;
        this.jgroupsNodeName = jgroupsNodeName;
        this.jgroupsClusterName = jgroupsClusterName;
        this.jgroupsInterface = jgroupsInterface;
    }

    public boolean enabled() {
        return enabled;
    }

    public boolean publisherEnabled() {
        return publisherEnabled;
    }

    public String publisherTransport() {
        return publisherTransport;
    }

    public String jgroupsProps() {
        return jgroupsProps;
    }

    public String jgroupsNodeName() {
        return jgroupsNodeName;
    }

    public String jgroupsClusterName() {
        return jgroupsClusterName;
    }

    public String jgroupsInterface() {
        return jgroupsInterface;
    }
}
