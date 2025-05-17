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
import eu.maveniverse.maven.mimir.shared.SessionConfig;
import java.nio.file.Path;

public class DaemonConfig {
    public static DaemonConfig with(SessionConfig sessionConfig) {
        requireNonNull(sessionConfig, "config");

        Path daemonBasedir = sessionConfig.basedir().resolve("daemon");
        Path socketPath = sessionConfig.basedir().resolve(Handle.DEFAULT_SOCKET_PATH);
        String systemNode = "file";

        if (sessionConfig.effectiveProperties().containsKey("mimir.daemon.socketPath")) {
            socketPath = SessionConfig.getCanonicalPath(sessionConfig
                    .basedir()
                    .resolve(sessionConfig.effectiveProperties().get("mimir.daemon.socketPath")));
        }
        if (sessionConfig.effectiveProperties().containsKey("mimir.daemon.systemNode")) {
            systemNode = sessionConfig.effectiveProperties().get("mimir.daemon.systemNode");
        }
        return new DaemonConfig(sessionConfig, daemonBasedir, socketPath, systemNode);
    }

    private final SessionConfig sessionConfig;
    private final Path daemonBasedir;
    private final Path socketPath;
    private final String systemNode;

    private DaemonConfig(SessionConfig sessionConfig, Path daemonBasedir, Path socketPath, String systemNode) {
        this.sessionConfig = requireNonNull(sessionConfig);
        this.daemonBasedir = requireNonNull(daemonBasedir);
        this.socketPath = requireNonNull(socketPath);
        this.systemNode = requireNonNull(systemNode);
    }

    public SessionConfig config() {
        return sessionConfig;
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
