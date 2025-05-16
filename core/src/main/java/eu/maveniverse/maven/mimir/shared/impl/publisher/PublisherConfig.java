/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.shared.impl.publisher;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.mimir.shared.Config;
import eu.maveniverse.maven.mimir.shared.impl.Utils;
import java.io.IOException;

public class PublisherConfig {
    public static PublisherConfig with(Config config) throws IOException {
        requireNonNull(config, "config");

        String hostAddress =
                Utils.getLocalHost(config.localHostHint().orElse(null)).getHostAddress();
        int hostPort = 0;

        if (config.effectiveProperties().containsKey("mimir.publisher.hostAddress")) {
            hostAddress = config.effectiveProperties().get("mimir.publisher.hostAddress");
        }
        if (config.effectiveProperties().containsKey("mimir.publisher.hostPort")) {
            hostPort = Integer.parseInt(config.effectiveProperties().get("mimir.publisher.hostPort"));
        }
        return new PublisherConfig(hostAddress, hostPort);
    }

    private final String hostAddress;
    private final int hostPort;

    private PublisherConfig(String hostAddress, int hostPort) {
        this.hostAddress = requireNonNull(hostAddress);
        this.hostPort = hostPort;
    }

    public String hostAddress() {
        return hostAddress;
    }

    public int hostPort() {
        return hostPort;
    }
}
