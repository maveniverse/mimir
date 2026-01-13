/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.node.ipfs;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

import eu.maveniverse.maven.mimir.shared.SessionConfig;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class IpfsNodeConfig {
    public static IpfsNodeConfig with(SessionConfig sessionConfig) throws IOException {
        requireNonNull(sessionConfig, "config");

        boolean enabled = false;
        boolean publisherEnabled = true;
        String multiaddr = "/ip4/127.0.0.1/tcp/5001";
        List<String> checksumAlgorithms = Arrays.asList("SHA-1", "SHA-512");

        if (sessionConfig.effectiveProperties().containsKey("mimir.ipfs.enabled")) {
            enabled = Boolean.parseBoolean(sessionConfig.effectiveProperties().get("mimir.ipfs.enabled"));
        }
        if (sessionConfig.effectiveProperties().containsKey("mimir.ipfs.publisher.enabled")) {
            publisherEnabled =
                    Boolean.parseBoolean(sessionConfig.effectiveProperties().get("mimir.ipfs.publisher.enabled"));
        }
        if (sessionConfig.effectiveProperties().containsKey("mimir.ipfs.multiaddr")) {
            multiaddr = sessionConfig.effectiveProperties().get("mimir.ipfs.multiaddr");
        }
        if (sessionConfig.effectiveProperties().containsKey("mimir.ipfs.checksumAlgorithms")) {
            checksumAlgorithms = Arrays.stream(sessionConfig
                            .effectiveProperties()
                            .get("mimir.ipfs.checksumAlgorithms")
                            .split(","))
                    .filter(s -> !s.trim().isEmpty())
                    .collect(toList());
        }
        return new IpfsNodeConfig(enabled, publisherEnabled, multiaddr, checksumAlgorithms);
    }

    public static final String NAME = "ipfs";

    private final boolean enabled;
    private final boolean publisherEnabled;
    private final String multiaddr;
    private final List<String> checksumAlgorithms;

    private IpfsNodeConfig(
            boolean enabled, boolean publisherEnabled, String multiaddr, List<String> checksumAlgorithms) {
        this.enabled = enabled;
        this.publisherEnabled = publisherEnabled;
        this.multiaddr = multiaddr;
        this.checksumAlgorithms = checksumAlgorithms;
    }

    public boolean enabled() {
        return enabled;
    }

    public boolean publisherEnabled() {
        return publisherEnabled;
    }

    public String multiaddr() {
        return multiaddr;
    }

    public List<String> checksumAlgorithms() {
        return checksumAlgorithms;
    }
}
