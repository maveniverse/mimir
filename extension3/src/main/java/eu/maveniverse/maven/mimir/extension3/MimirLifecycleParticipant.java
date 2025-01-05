/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.extension3;

import eu.maveniverse.maven.mimir.shared.Config;
import eu.maveniverse.maven.mimir.shared.SessionFactory;
import eu.maveniverse.maven.mimir.shared.impl.Utils;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.execution.MavenSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResult;
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
            mayResolverDaemonArtifact(
                    config,
                    repoSession,
                    Collections.singletonList(
                            new RemoteRepository.Builder("central", "default", "https://repo.maven.apache.org/maven2/")
                                    .build()));
            MimirUtils.seedSession(session.getRepositorySession(), sessionFactory.createSession(config));
            logger.info("Mimir {} session created", config.mimirVersion());
        } catch (Exception e) {
            throw new MavenExecutionException("Error creating Mimir session", e);
        }
    }

    @Override
    public void afterSessionEnd(MavenSession session) throws MavenExecutionException {
        try {
            MimirUtils.closeSession(session.getRepositorySession());
        } catch (Exception e) {
            throw new MavenExecutionException("Error closing Mimir session", e);
        }
    }

    private void mayResolverDaemonArtifact(
            Config config, RepositorySystemSession session, List<RemoteRepository> remoteRepositories)
            throws MavenExecutionException {
        Path daemonJarPath = config.basedir().resolve("daemon-" + config.mimirVersion() + ".jar");
        if (!Files.exists(daemonJarPath)) {
            try {
                logger.info("Resolving Mimir daemon version {}", config.mimirVersion());
                ArtifactRequest artifactRequest = new ArtifactRequest(
                        new DefaultArtifact("eu.maveniverse.maven.mimir:daemon:jar:daemon:" + config.mimirVersion()),
                        remoteRepositories,
                        "mimir");
                ArtifactResult artifactResult = repositorySystem.resolveArtifact(session, artifactRequest);
                Utils.copyOrLink(artifactResult.getArtifact().getFile().toPath(), daemonJarPath);
            } catch (Exception e) {
                throw new MavenExecutionException("Error resolving daemon jar", e);
            }
        }
    }
}
