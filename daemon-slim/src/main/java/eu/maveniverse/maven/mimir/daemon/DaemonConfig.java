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
import eu.maveniverse.maven.mimir.shared.impl.ParseUtils;
import eu.maveniverse.maven.shared.core.fs.FileUtils;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.aether.artifact.DefaultArtifact;

public class DaemonConfig {
    public static DaemonConfig with(SessionConfig sessionConfig) {
        requireNonNull(sessionConfig, "config");

        Path locks = sessionConfig.basedir().resolve("locks");
        Path daemonLockDir = locks.resolve("daemon");
        Path socketPath = sessionConfig.basedir().resolve(Handle.DEFAULT_SOCKET_PATH);
        String systemNode = "file";
        boolean preSeedItself = false;
        ArrayList<ParseUtils.ArtifactSource> itselfArtifacts = new ArrayList<>();
        // TODO: this may need to be centralized, and later even discovered (ie extension4?)
        itselfArtifacts.add(new ParseUtils.ArtifactSource(
                ParseUtils.CENTRAL,
                new DefaultArtifact("eu.maveniverse.maven.mimir:extension3:" + sessionConfig.mimirVersion())));
        ArrayList<ParseUtils.ArtifactSource> preSeedArtifacts = new ArrayList<>();

        if (sessionConfig.effectiveProperties().containsKey("mimir.daemon.socketPath")) {
            socketPath = FileUtils.canonicalPath(sessionConfig
                    .basedir()
                    .resolve(sessionConfig.effectiveProperties().get("mimir.daemon.socketPath")));
        }
        if (sessionConfig.effectiveProperties().containsKey("mimir.daemon.systemNode")) {
            systemNode = sessionConfig.effectiveProperties().get("mimir.daemon.systemNode");
        }
        if (sessionConfig.effectiveProperties().containsKey("mimir.daemon.preSeedItself")) {
            preSeedItself =
                    Boolean.parseBoolean(sessionConfig.effectiveProperties().get("mimir.daemon.preSeedItself"));
        }
        if (sessionConfig.effectiveProperties().containsKey("mimir.daemon.itselfArtifacts")) {
            itselfArtifacts.clear();
            itselfArtifacts.addAll(ParseUtils.parseBundleSources(
                    sessionConfig, sessionConfig.effectiveProperties().get("mimir.daemon.itselfArtifacts"), false));
        }
        if (sessionConfig.effectiveProperties().containsKey("mimir.daemon.preSeedArtifacts")) {
            preSeedArtifacts.addAll(ParseUtils.parseBundleSources(
                    sessionConfig, sessionConfig.effectiveProperties().get("mimir.daemon.preSeedArtifacts"), false));
        }
        return new DaemonConfig(
                sessionConfig, daemonLockDir, socketPath, systemNode, preSeedItself, itselfArtifacts, preSeedArtifacts);
    }

    private final SessionConfig sessionConfig;
    private final Path daemonLockDir;
    private final Path socketPath;
    private final String systemNode;
    private final boolean preSeedItself;
    private final List<ParseUtils.ArtifactSource> itselfArtifacts;
    private final List<ParseUtils.ArtifactSource> preSeedArtifacts;

    private DaemonConfig(
            SessionConfig sessionConfig,
            Path daemonLockDir,
            Path socketPath,
            String systemNode,
            boolean preSeedItself,
            List<ParseUtils.ArtifactSource> itselfArtifacts,
            List<ParseUtils.ArtifactSource> preSeedArtifacts) {
        this.sessionConfig = requireNonNull(sessionConfig);
        this.daemonLockDir = requireNonNull(daemonLockDir);
        this.socketPath = requireNonNull(socketPath);
        this.systemNode = requireNonNull(systemNode);
        this.preSeedItself = preSeedItself;
        this.itselfArtifacts = requireNonNull(itselfArtifacts);
        this.preSeedArtifacts = requireNonNull(preSeedArtifacts);
    }

    public SessionConfig config() {
        return sessionConfig;
    }

    public Path daemonLockDir() {
        return daemonLockDir;
    }

    public Path socketPath() {
        return socketPath;
    }

    public String systemNode() {
        return systemNode;
    }

    public boolean preSeedItself() {
        return preSeedItself;
    }

    public List<ParseUtils.ArtifactSource> itselfArtifacts() {
        return itselfArtifacts;
    }

    public List<ParseUtils.ArtifactSource> preSeedArtifacts() {
        return preSeedArtifacts;
    }
}
