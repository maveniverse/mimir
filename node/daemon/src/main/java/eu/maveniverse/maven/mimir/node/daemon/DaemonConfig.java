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

        Path daemonBasedir = config.basedir().resolve("daemon");
        Path socketPath = config.basedir().resolve(Handle.DEFAULT_SOCKET_PATH);
        Path daemonJavaHome = Path.of(config.effectiveProperties()
                .getOrDefault(
                        "mimir.daemon.java.home", config.effectiveProperties().get("java.home")));
        boolean autoupdate = mimirVersionPresent; // without version GAV is wrong
        boolean autostart = mimirVersionPresent;
        Duration autostartDuration = Duration.ofMinutes(1);
        boolean autostop = false;
        Path daemonJar = config.basedir().resolve("daemon-" + mimirVersion + ".jar");
        Path daemonLog = config.basedir().resolve("daemon-" + mimirVersion + ".log");
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
        if (config.effectiveProperties().containsKey("mimir.daemon.daemonJar")) {
            daemonJar = config.basedir().resolve(config.effectiveProperties().get("mimir.daemon.daemonJar"));
        }
        if (config.effectiveProperties().containsKey("mimir.daemon.daemonLog")) {
            daemonLog = config.basedir().resolve(config.effectiveProperties().get("mimir.daemon.daemonLog"));
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
                config,
                daemonBasedir,
                socketPath,
                daemonJavaHome,
                autoupdate,
                autostart,
                autostartDuration,
                autostop,
                daemonJar,
                daemonLog,
                daemonGav,
                passOnBasedir,
                debug);
    }

    public static final String NAME = "daemon";

    private final Config config;
    private final Path daemonBasedir;
    private final Path socketPath;
    private final Path daemonJavaHome;
    private final boolean autoupdate;
    private final boolean autostart;
    private final Duration autostartDuration;
    private final boolean autostop;
    private final Path daemonJar;
    private final Path daemonLog;
    private final String daemonGav;
    private final boolean passOnBasedir;
    private final boolean debug;

    private DaemonConfig(
            Config config,
            Path daemonBasedir,
            Path socketPath,
            Path daemonJavaHome,
            boolean autoupdate,
            boolean autostart,
            Duration autostartDuration,
            boolean autostop,
            Path daemonJar,
            Path daemonLog,
            String daemonGav,
            boolean passOnBasedir,
            boolean debug) {
        this.config = requireNonNull(config);
        this.daemonBasedir = requireNonNull(daemonBasedir);
        this.socketPath = requireNonNull(socketPath);
        this.daemonJavaHome = requireNonNull(daemonJavaHome);
        this.autoupdate = autoupdate;
        this.autostart = autostart;
        this.autostartDuration = requireNonNull(autostartDuration);
        this.autostop = autostop;
        this.daemonJar = requireNonNull(daemonJar);
        this.daemonLog = requireNonNull(daemonLog);
        this.daemonGav = requireNonNull(daemonGav);
        this.passOnBasedir = passOnBasedir;
        this.debug = debug;
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

    public Path daemonJar() {
        return daemonJar;
    }

    public Path daemonLog() {
        return daemonLog;
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
