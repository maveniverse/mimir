/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.extension3;

import eu.maveniverse.maven.mimir.shared.Session;
import java.util.List;
import java.util.Optional;
import javax.inject.Named;
import javax.inject.Singleton;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.spi.resolution.ArtifactResolverPostProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@Named
public class MimirArtifactResolverPostProcessor implements ArtifactResolverPostProcessor {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public void postProcess(RepositorySystemSession session, List<ArtifactResult> artifactResults) {
        for (ArtifactResult artifactResult : artifactResults) {
            if (artifactResult.isResolved()
                    && artifactResult.getRepository() instanceof RemoteRepository remoteRepository) {
                Optional<Session> sessionOptional = MimirUtils.mayGetSession(session);
                if (sessionOptional.isPresent()) {
                    Artifact artifact = artifactResult.getArtifact();
                    String requestContext = artifactResult.getRequest().getRequestContext();
                    Session ms = sessionOptional.orElseThrow();
                    if (ms.repositorySupported(remoteRepository) && ms.artifactSupported(artifact)) {
                        // do something
                        logger.info("{} @ {} < {}", artifact, requestContext, remoteRepository);
                    }
                }
            }
        }
    }
}
