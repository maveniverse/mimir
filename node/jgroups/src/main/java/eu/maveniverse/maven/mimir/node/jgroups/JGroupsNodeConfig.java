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
import java.io.IOException;
import java.net.InetAddress;

public class JGroupsNodeConfig {
    public static JGroupsNodeConfig with(SessionConfig sessionConfig) throws IOException {
        requireNonNull(sessionConfig, "config");

        String groupSuffix = "@n/a";
        if (!SessionConfig.UNKNOWN_VERSION.equals(sessionConfig.mimirVersion())) {
            String version = sessionConfig.mimirVersion();
            if (version.endsWith("-SNAPSHOT")) {
                groupSuffix = "@" + version;
            } else {
                groupSuffix = "@" + version.substring(0, version.lastIndexOf('.'));
            }
        }

        boolean enabled = true;
        boolean publisherEnabled = true;
        String publisherTransport = "socket";
        String jgroupsProps = "udp-new.xml";
        String jgroupsNodeName = InetAddress.getLocalHost().getHostName();
        String jgroupsClusterName = "mimir-jgroups" + groupSuffix;
        String jgroupsInterface = null;

        if (sessionConfig.effectiveProperties().containsKey("mimir.jgroups.enabled")) {
            enabled = Boolean.parseBoolean(sessionConfig.effectiveProperties().get("mimir.jgroups.enabled"));
        }
        if (sessionConfig.effectiveProperties().containsKey("mimir.jgroups.publisher.enabled")) {
            publisherEnabled =
                    Boolean.parseBoolean(sessionConfig.effectiveProperties().get("mimir.jgroups.publisher.enabled"));
        }
        if (sessionConfig.effectiveProperties().containsKey("mimir.jgroups.publisher.transport")) {
            publisherTransport = sessionConfig.effectiveProperties().get("mimir.jgroups.publisher.transport");
        }
        if (sessionConfig.effectiveProperties().containsKey("mimir.jgroups.props")) {
            jgroupsProps = sessionConfig.effectiveProperties().get("mimir.jgroups.props");
        }
        if (sessionConfig.effectiveProperties().containsKey("mimir.jgroups.nodeName")) {
            jgroupsNodeName = sessionConfig.effectiveProperties().get("mimir.jgroups.nodeName");
        }
        if (sessionConfig.effectiveProperties().containsKey("mimir.jgroups.clusterName")) {
            jgroupsClusterName = sessionConfig.effectiveProperties().get("mimir.jgroups.clusterName");
        }
        if (sessionConfig.effectiveProperties().containsKey("mimir.jgroups.interface")) {
            jgroupsInterface = sessionConfig.effectiveProperties().get("mimir.jgroups.interface");
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
