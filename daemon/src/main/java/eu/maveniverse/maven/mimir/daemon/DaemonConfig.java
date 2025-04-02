/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.daemon;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.mimir.shared.Config;
import java.nio.file.Path;

public class DaemonConfig {
    public static DaemonConfig with(Config config) {
        requireNonNull(config, "config");

        Path socketPath = config.basedir().resolve("mimir-socket");
        String systemNode = "file";

        if (config.effectiveProperties().containsKey("mimir.daemon.socketPath")) {
            socketPath = Config.getCanonicalPath(
                    config.basedir().resolve(config.effectiveProperties().get("mimir.daemon.socketPath")));
        }
        if (config.effectiveProperties().containsKey("mimir.daemon.systemNode")) {
            systemNode = config.effectiveProperties().get("mimir.daemon.systemNode");
        }
        return new DaemonConfig(socketPath, systemNode);
    }

    private final Path socketPath;
    private final String systemNode;

    private DaemonConfig(Path socketPath, String systemNode) {
        this.socketPath = requireNonNull(socketPath);
        this.systemNode = requireNonNull(systemNode);
    }

    public Path socketPath() {
        return socketPath;
    }

    public String systemNode() {
        return systemNode;
    }
}
