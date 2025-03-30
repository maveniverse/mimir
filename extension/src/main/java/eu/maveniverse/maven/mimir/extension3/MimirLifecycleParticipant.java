/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.extension3;

import eu.maveniverse.maven.mimir.node.daemon.DaemonConfig;
import eu.maveniverse.maven.mimir.shared.Config;
import eu.maveniverse.maven.mimir.shared.SessionFactory;
import eu.maveniverse.maven.mimir.shared.impl.Utils;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.RepositoryUtils;
import org.apache.maven.execution.MavenSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.VersionRangeRequest;
import org.eclipse.aether.resolution.VersionRangeResult;
import org.eclipse.aether.version.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Lifecycle participant that creates Mimir session early, as it may be costly operation (spawning a new Java process,
 * or connecting to some cluster or whatever).
 */
@Singleton
@Named
public class MimirLifecycleParticipant extends AbstractMavenLifecycleParticipant {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final RepositorySystem repositorySystem;
    private final SessionFactory sessionFactory;

    @Inject
    public MimirLifecycleParticipant(RepositorySystem repositorySystem, SessionFactory sessionFactory) {
        this.repositorySystem = repositorySystem;
        this.sessionFactory = sessionFactory;
    }

    @Override
    public void afterSessionStart(MavenSession session) throws MavenExecutionException {
        try {
            RepositorySystemSession repoSession = session.getRepositorySession();
            Config config = Config.defaults()
                    .userProperties(repoSession.getUserProperties())
                    .systemProperties(repoSession.getSystemProperties())
                    .build();
            MimirUtils.setConfig(repoSession, config);
            if (MimirUtils.isEnabled(repoSession)) {
                List<RemoteRepository> remoteRepositories = RepositoryUtils.toRepos(
                        session.getProjectBuildingRequest().getRemoteRepositories());
                mayCheckForUpdates(config, repoSession, remoteRepositories);
                mayResolveDaemonArtifact(config, repoSession, remoteRepositories);
                MimirUtils.setSession(session.getRepositorySession(), sessionFactory.createSession(config));
            } else {
                logger.info("Mimir is disabled");
            }
        } catch (Exception e) {
            throw new MavenExecutionException("Error creating Mimir session", e);
        }
    }

    @Override
    public void afterSessionEnd(MavenSession session) throws MavenExecutionException {
        try {
            if (MimirUtils.isEnabled(session.getRepositorySession())) {
                MimirUtils.requireSession(session.getRepositorySession()).close();
            }
        } catch (Exception e) {
            throw new MavenExecutionException("Error closing Mimir session", e);
        }
    }

    private void mayResolveDaemonArtifact(
            Config config, RepositorySystemSession session, List<RemoteRepository> remoteRepositories) {
        DaemonConfig daemonConfig = DaemonConfig.with(config);
        if (!daemonConfig.autostart() || config.mimirVersion().isEmpty()) {
            logger.debug("Not resolving Mimir daemon; autostart not enabled or version not detected");
            return;
        }
        Path daemonJarPath = config.basedir().resolve(daemonConfig.daemonJarName());
        if (!Files.exists(daemonJarPath)) {
            try {
                logger.info(
                        "Resolving Mimir daemon version {}",
                        config.mimirVersion().orElseThrow());
                ArtifactRequest artifactRequest =
                        new ArtifactRequest(new DefaultArtifact(daemonConfig.daemonGav()), remoteRepositories, "mimir");
                ArtifactResult artifactResult = repositorySystem.resolveArtifact(session, artifactRequest);
                Files.createDirectories(daemonJarPath.getParent());
                Utils.copyOrLink(artifactResult.getArtifact().getFile().toPath(), daemonJarPath);
            } catch (Exception e) {
                logger.warn("Failed to resolve daemon: {}", e.getMessage());
            }
        }
    }

    private void mayCheckForUpdates(
            Config config, RepositorySystemSession session, List<RemoteRepository> remoteRepositories) {
        DaemonConfig daemonConfig = DaemonConfig.with(config);
        if (!daemonConfig.autoupdate() || config.mimirVersion().isEmpty()) {
            logger.debug("Not checking for Mimir updates; not enabled or version not detected");
            return;
        }
        try {
            String mimirVersion = config.mimirVersion().orElseThrow();
            logger.debug("Checking for Mimir updates...");
            VersionRangeRequest versionRangeRequest = new VersionRangeRequest(
                    new DefaultArtifact(daemonConfig.daemonGav()).setVersion("[" + mimirVersion + ",)"),
                    remoteRepositories,
                    "mimir");
            VersionRangeResult rangeResult = repositorySystem.resolveVersionRange(session, versionRangeRequest);
            List<Version> versions = rangeResult.getVersions();
            if (versions.size() > 1) {
                String latest = versions.get(versions.size() - 1).toString();
                // only for locally built versions; we do not publish snapshots
                if (!latest.endsWith("-SNAPSHOT") && !Objects.equals(mimirVersion, latest)) {
                    logger.info("Please upgrade to Mimir version {} (you are using version {})", latest, mimirVersion);
                }
            } else {
                logger.debug("Mimir {} is up to date", mimirVersion);
            }
        } catch (Exception e) {
            logger.warn("Failed to check for updates; ignoring it", e);
        }
    }
}
