/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.node.daemon;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.mimir.shared.Config;
import java.nio.file.Path;

public class UdsNodeConfig {
    public static UdsNodeConfig with(Config config) {
        requireNonNull(config, "config");

        boolean enabled = true;
        Path socketPath = config.basedir().resolve("uds-socket");
        boolean autostart = true;
        if (config.effectiveProperties().containsKey("mimir.uds.enabled")) {
            enabled = Boolean.parseBoolean(config.effectiveProperties().get("mimir.uds.enabled"));
        }
        if (config.effectiveProperties().containsKey("mimir.uds.socketPath")) {
            socketPath =
                    Config.getCanonicalPath(Path.of(config.effectiveProperties().get("mimir.uds.socketPath")));
        }
        if (config.effectiveProperties().containsKey("mimir.uds.autostart")) {
            autostart = Boolean.parseBoolean(config.effectiveProperties().get("mimir.uds.autostart"));
        }
        return new UdsNodeConfig(enabled, socketPath, autostart);
    }

    public static UdsNodeConfig of(boolean enabled, Path socketPath, boolean autostart) {
        return new UdsNodeConfig(enabled, Config.getCanonicalPath(socketPath), autostart);
    }

    public static final String NAME = "uds";

    private final boolean enabled;
    private final Path socketPath;
    private final boolean autostart;

    private UdsNodeConfig(boolean enabled, Path socketPath, boolean autostart) {
        this.enabled = enabled;
        this.socketPath = socketPath;
        this.autostart = autostart;
    }

    public boolean enabled() {
        return enabled;
    }

    public Path socketPath() {
        return socketPath;
    }

    public boolean autostart() {
        return autostart;
    }
}
