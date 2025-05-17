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
import eu.maveniverse.maven.mimir.shared.SessionConfig;
import java.nio.file.Path;
import java.time.Duration;

public class DaemonConfig {
    public static DaemonConfig with(SessionConfig sessionConfig) {
        requireNonNull(sessionConfig, "config");

        final boolean mimirVersionPresent = sessionConfig.mimirVersion().isPresent();
        final String mimirVersion = sessionConfig.mimirVersion().orElse("UNKNOWN");

        Path daemonBasedir = sessionConfig.basedir().resolve("daemon");
        Path socketPath = sessionConfig.basedir().resolve(Handle.DEFAULT_SOCKET_PATH);
        Path daemonJavaHome = Path.of(sessionConfig
                .effectiveProperties()
                .getOrDefault(
                        "mimir.daemon.java.home",
                        sessionConfig.effectiveProperties().get("java.home")));
        boolean autoupdate = mimirVersionPresent; // without version GAV is wrong
        boolean autostart = mimirVersionPresent;
        Duration autostartDuration = Duration.ofMinutes(1);
        boolean autostop = false;
        Path daemonJar = sessionConfig.basedir().resolve("daemon-" + mimirVersion + ".jar");
        Path daemonLog = sessionConfig.basedir().resolve("daemon-" + mimirVersion + ".log");
        String daemonGav = "eu.maveniverse.maven.mimir:daemon:jar:daemon:" + mimirVersion;
        boolean passOnBasedir = false;
        boolean debug = false;

        if (sessionConfig.effectiveProperties().containsKey("mimir.daemon.socketPath")) {
            socketPath = SessionConfig.getCanonicalPath(
                    Path.of(sessionConfig.effectiveProperties().get("mimir.daemon.socketPath")));
        }
        if (sessionConfig.effectiveProperties().containsKey("mimir.daemon.autoupdate")) {
            autoupdate =
                    Boolean.parseBoolean(sessionConfig.effectiveProperties().get("mimir.daemon.autoupdate"));
        }
        if (sessionConfig.effectiveProperties().containsKey("mimir.daemon.autostart")) {
            autostart = Boolean.parseBoolean(sessionConfig.effectiveProperties().get("mimir.daemon.autostart"));
        }
        if (sessionConfig.effectiveProperties().containsKey("mimir.daemon.autostartDuration")) {
            autostartDuration =
                    Duration.parse(sessionConfig.effectiveProperties().get("mimir.daemon.autostartDuration"));
        }
        if (sessionConfig.effectiveProperties().containsKey("mimir.daemon.autostop")) {
            autostop = Boolean.parseBoolean(sessionConfig.effectiveProperties().get("mimir.daemon.autostop"));
        }
        if (sessionConfig.effectiveProperties().containsKey("mimir.daemon.daemonJar")) {
            daemonJar = sessionConfig
                    .basedir()
                    .resolve(sessionConfig.effectiveProperties().get("mimir.daemon.daemonJar"));
        }
        if (sessionConfig.effectiveProperties().containsKey("mimir.daemon.daemonLog")) {
            daemonLog = sessionConfig
                    .basedir()
                    .resolve(sessionConfig.effectiveProperties().get("mimir.daemon.daemonLog"));
        }
        if (sessionConfig.effectiveProperties().containsKey("mimir.daemon.daemonGav")) {
            daemonGav = sessionConfig.effectiveProperties().get("mimir.daemon.daemonGav");
        }
        if (sessionConfig.effectiveProperties().containsKey("mimir.daemon.passOnBasedir")) {
            passOnBasedir =
                    Boolean.parseBoolean(sessionConfig.effectiveProperties().get("mimir.daemon.passOnBasedir"));
        }
        if (sessionConfig.effectiveProperties().containsKey("mimir.daemon.debug")) {
            debug = Boolean.parseBoolean(sessionConfig.effectiveProperties().get("mimir.daemon.debug"));
        }
        return new DaemonConfig(
                sessionConfig,
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

    private final SessionConfig sessionConfig;
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
            SessionConfig sessionConfig,
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
        this.sessionConfig = requireNonNull(sessionConfig);
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

    public SessionConfig config() {
        return sessionConfig;
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
