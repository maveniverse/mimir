/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.extension3;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.mimir.shared.Session;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.spi.connector.checksum.ChecksumAlgorithmFactorySelector;
import org.eclipse.aether.spi.connector.checksum.ChecksumAlgorithmHelper;
import org.eclipse.aether.spi.resolution.ArtifactResolverPostProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@Named
public class MimirArtifactResolverPostProcessor implements ArtifactResolverPostProcessor {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final ChecksumAlgorithmFactorySelector checksumAlgorithmFactorySelector;

    @Inject
    public MimirArtifactResolverPostProcessor(ChecksumAlgorithmFactorySelector checksumAlgorithmFactorySelector) {
        this.checksumAlgorithmFactorySelector =
                requireNonNull(checksumAlgorithmFactorySelector, "checksumAlgorithmFactorySelector");
    }

    @Override
    public void postProcess(RepositorySystemSession session, List<ArtifactResult> artifactResults) {
        for (ArtifactResult artifactResult : artifactResults) {
            if (artifactResult.getRepository() instanceof RemoteRepository remoteRepository) {
                Optional<Session> sessionOptional = MimirUtils.mayGetSession(session);
                if (sessionOptional.isPresent()) {
                    Session ms = sessionOptional.orElseThrow();
                    Artifact artifact = artifactResult.getArtifact();
                    boolean resolved = artifactResult.isResolved();
                    if (resolved && ms.repositorySupported(remoteRepository) && ms.artifactSupported(artifact)) {
                        try {
                            // store it; if needed
                            if (!ms.retrievedFromCache(remoteRepository, artifact)
                                    && !ms.storedToCache(remoteRepository, artifact)) {
                                ms.store(
                                        remoteRepository,
                                        artifact,
                                        artifact.getFile().toPath(),
                                        Map.of(),
                                        ChecksumAlgorithmHelper.calculate(
                                                artifact.getFile(),
                                                checksumAlgorithmFactorySelector.selectList(ms.checksumAlgorithms())));
                            }
                            // mark it
                            ms.mark(
                                    remoteRepository,
                                    artifact,
                                    Map.of(
                                            "context",
                                            artifactResult.getRequest().getRequestContext()));
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    }
                }
            }
        }
    }
}
