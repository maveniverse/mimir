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

public class DaemonConfig {
    public static Config toDaemonProcessConfig(Config config) {
        return config.toBuilder()
                .setUserProperty("mimir.daemon.enabled", "false")
                .build();
    }

    public static DaemonConfig with(Config config) {
        requireNonNull(config, "config");

        final String mimirVersion = config.mimirVersion().orElse("UNKNOWN");

        Path socketPath = config.basedir().resolve("mimir-socket");
        boolean autoupdate = true;
        boolean autostart = true;
        String daemonJarName = "daemon-" + mimirVersion + ".jar";
        String daemonLogName = "daemon-" + mimirVersion + ".log";
        String daemonGav = "eu.maveniverse.maven.mimir:daemon:jar:daemon:" + mimirVersion;
        String systemNode = "file";

        if (config.effectiveProperties().containsKey("mimir.daemon.socketPath")) {
            socketPath =
                    Config.getCanonicalPath(Path.of(config.effectiveProperties().get("mimir.daemon.socketPath")));
        }
        if (config.effectiveProperties().containsKey("mimir.daemon.autoupdate")) {
            autoupdate = Boolean.parseBoolean(config.effectiveProperties().get("mimir.daemon.autoupdate"));
        }
        if (config.effectiveProperties().containsKey("mimir.daemon.autostart")) {
            autostart = Boolean.parseBoolean(config.effectiveProperties().get("mimir.daemon.autostart"));
        }
        if (config.effectiveProperties().containsKey("mimir.daemon.daemonJarName")) {
            daemonJarName = config.effectiveProperties().get("mimir.daemon.daemonJarName");
        }
        if (config.effectiveProperties().containsKey("mimir.daemon.daemonLogName")) {
            daemonLogName = config.effectiveProperties().get("mimir.daemon.daemonLogName");
        }
        if (config.effectiveProperties().containsKey("mimir.daemon.daemonGav")) {
            daemonGav = config.effectiveProperties().get("mimir.daemon.daemonGav");
        }
        if (config.effectiveProperties().containsKey("mimir.daemon.systemNode")) {
            systemNode = config.effectiveProperties().get("mimir.daemon.systemNode");
        }
        return new DaemonConfig(socketPath, autoupdate, autostart, daemonJarName, daemonLogName, daemonGav, systemNode);
    }

    public static final String NAME = "daemon";

    private final Path socketPath;
    private final boolean autoupdate;
    private final boolean autostart;
    private final String daemonJarName;
    private final String daemonLogName;
    private final String daemonGav;
    private final String systemNode;

    private DaemonConfig(
            Path socketPath,
            boolean autoupdate,
            boolean autostart,
            String daemonJarName,
            String daemonLogName,
            String daemonGav,
            String systemNode) {
        this.socketPath = socketPath;
        this.autoupdate = autoupdate;
        this.autostart = autostart;
        this.daemonJarName = daemonJarName;
        this.daemonLogName = daemonLogName;
        this.daemonGav = daemonGav;
        this.systemNode = systemNode;
    }

    public Path socketPath() {
        return socketPath;
    }

    public boolean autoupdate() {
        return autoupdate;
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

    public String systemNode() {
        return systemNode;
    }
}
