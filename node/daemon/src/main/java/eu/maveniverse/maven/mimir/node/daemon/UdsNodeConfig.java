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
        String daemonJarName = "daemon-" + config.mimirVersion() + ".jar";
        String daemonLogName = "daemon-" + config.mimirVersion() + ".log";
        String daemonGav = "eu.maveniverse.maven.mimir:daemon:jar:daemon:" + config.mimirVersion();
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
        if (config.effectiveProperties().containsKey("mimir.uds.daemonJarName")) {
            daemonJarName = config.effectiveProperties().get("mimir.uds.daemonJarName");
        }
        if (config.effectiveProperties().containsKey("mimir.uds.daemonLogName")) {
            daemonLogName = config.effectiveProperties().get("mimir.uds.daemonLogName");
        }
        if (config.effectiveProperties().containsKey("mimir.uds.daemonGav")) {
            daemonGav = config.effectiveProperties().get("mimir.uds.daemonGav");
        }
        return new UdsNodeConfig(enabled, socketPath, autostart, daemonJarName, daemonLogName, daemonGav);
    }

    public static final String NAME = "uds";

    private final boolean enabled;
    private final Path socketPath;
    private final boolean autostart;
    private final String daemonJarName;
    private final String daemonLogName;
    private final String daemonGav;

    private UdsNodeConfig(
            boolean enabled,
            Path socketPath,
            boolean autostart,
            String daemonJarName,
            String daemonLogName,
            String daemonGav) {
        this.enabled = enabled;
        this.socketPath = socketPath;
        this.autostart = autostart;
        this.daemonJarName = daemonJarName;
        this.daemonLogName = daemonLogName;
        this.daemonGav = daemonGav;
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

    public String daemonJarName() {
        return daemonJarName;
    }

    public String daemonLogName() {
        return daemonLogName;
    }

    public String daemonGav() {
        return daemonGav;
    }
}
