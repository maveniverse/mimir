/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.daemon;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.mimir.daemon.protocol.Handle;
import eu.maveniverse.maven.mimir.shared.Config;
import java.nio.file.Path;

public class DaemonConfig {
    public static DaemonConfig with(Config config) {
        requireNonNull(config, "config");

        Path daemonBasedir = config.basedir().resolve("daemon");
        Path socketPath = config.basedir().resolve(Handle.DEFAULT_SOCKET_PATH);
        String systemNode = "file";

        if (config.effectiveProperties().containsKey("mimir.daemon.socketPath")) {
            socketPath = Config.getCanonicalPath(
                    config.basedir().resolve(config.effectiveProperties().get("mimir.daemon.socketPath")));
        }
        if (config.effectiveProperties().containsKey("mimir.daemon.systemNode")) {
            systemNode = config.effectiveProperties().get("mimir.daemon.systemNode");
        }
        return new DaemonConfig(config, daemonBasedir, socketPath, systemNode);
    }

    private final Config config;
    private final Path daemonBasedir;
    private final Path socketPath;
    private final String systemNode;

    private DaemonConfig(Config config, Path daemonBasedir, Path socketPath, String systemNode) {
        this.config = requireNonNull(config);
        this.daemonBasedir = requireNonNull(daemonBasedir);
        this.socketPath = requireNonNull(socketPath);
        this.systemNode = requireNonNull(systemNode);
    }

    public Config config() {
        return config;
    }

    public Path daemonBasedir() {
        return daemonBasedir;
    }

    public Path socketPath() {
        return socketPath;
    }

    public String systemNode() {
        return systemNode;
    }
}
