/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.node.daemon;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.mimir.daemon.protocol.Handle;
import eu.maveniverse.maven.mimir.shared.Config;
import java.nio.file.Path;
import java.time.Duration;

public class DaemonConfig {
    public static DaemonConfig with(Config config) {
        requireNonNull(config, "config");

        final boolean mimirVersionPresent = config.mimirVersion().isPresent();
        final String mimirVersion = config.mimirVersion().orElse("UNKNOWN");

        Path socketPath = config.basedir().resolve(Handle.DEFAULT_SOCKET_PATH);
        Path daemonJavaHome = Path.of(config.effectiveProperties()
                .getOrDefault(
                        "mimir.daemon.java.home", config.effectiveProperties().get("java.home")));
        boolean autoupdate = mimirVersionPresent; // without version GAV is wrong
        boolean autostart = mimirVersionPresent;
        Duration autostartDuration = Duration.ofMinutes(1);
        boolean autostop = false;
        String daemonJarName = "daemon-" + mimirVersion + ".jar";
        String daemonLogName = "daemon-" + mimirVersion + ".log";
        String daemonGav = "eu.maveniverse.maven.mimir:daemon:jar:daemon:" + mimirVersion;
        boolean passOnBasedir = false;
        boolean debug = false;

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
        if (config.effectiveProperties().containsKey("mimir.daemon.autostartDuration")) {
            autostartDuration = Duration.parse(config.effectiveProperties().get("mimir.daemon.autostartDuration"));
        }
        if (config.effectiveProperties().containsKey("mimir.daemon.autostop")) {
            autostop = Boolean.parseBoolean(config.effectiveProperties().get("mimir.daemon.autostop"));
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
        if (config.effectiveProperties().containsKey("mimir.daemon.passOnBasedir")) {
            passOnBasedir = Boolean.parseBoolean(config.effectiveProperties().get("mimir.daemon.passOnBasedir"));
        }
        if (config.effectiveProperties().containsKey("mimir.daemon.debug")) {
            debug = Boolean.parseBoolean(config.effectiveProperties().get("mimir.daemon.debug"));
        }
        return new DaemonConfig(
                socketPath,
                daemonJavaHome,
                autoupdate,
                autostart,
                autostartDuration,
                autostop,
                daemonJarName,
                daemonLogName,
                daemonGav,
                passOnBasedir,
                debug);
    }

    public static final String NAME = "daemon";

    private final Path socketPath;
    private final Path daemonJavaHome;
    private final boolean autoupdate;
    private final boolean autostart;
    private final Duration autostartDuration;
    private final boolean autostop;
    private final String daemonJarName;
    private final String daemonLogName;
    private final String daemonGav;
    private final boolean passOnBasedir;
    private final boolean debug;

    private DaemonConfig(
            Path socketPath,
            Path daemonJavaHome,
            boolean autoupdate,
            boolean autostart,
            Duration autostartDuration,
            boolean autostop,
            String daemonJarName,
            String daemonLogName,
            String daemonGav,
            boolean passOnBasedir,
            boolean debug) {
        this.socketPath = requireNonNull(socketPath);
        this.daemonJavaHome = requireNonNull(daemonJavaHome);
        this.autoupdate = autoupdate;
        this.autostart = autostart;
        this.autostartDuration = requireNonNull(autostartDuration);
        this.autostop = autostop;
        this.daemonJarName = requireNonNull(daemonJarName);
        this.daemonLogName = requireNonNull(daemonLogName);
        this.daemonGav = requireNonNull(daemonGav);
        this.passOnBasedir = passOnBasedir;
        this.debug = debug;
    }

    public Path socketPath() {
        return socketPath;
    }

    public Path daemonJavaHome() {
        return daemonJavaHome;
    }

    public boolean autoupdate() {
        return autoupdate;
    }

    public boolean autostart() {
        return autostart;
    }

    public Duration autostartDuration() {
        return autostartDuration;
    }

    public boolean autostop() {
        return autostop;
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

    public boolean passOnBasedir() {
        return passOnBasedir;
    }

    public boolean debug() {
        return debug;
    }
}
