/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.extension3;

import eu.maveniverse.maven.mimir.shared.MimirUtils;
import eu.maveniverse.maven.shared.core.component.ComponentSupport;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Collections;
import java.util.List;
import javax.inject.Named;
import javax.inject.Singleton;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.spi.resolution.ArtifactResolverPostProcessor;

@Singleton
@Named
public class MimirArtifactResolverPostProcessor extends ComponentSupport implements ArtifactResolverPostProcessor {
    @Override
    public void postProcess(RepositorySystemSession session, List<ArtifactResult> artifactResults) {
        MimirUtils.mayGetSession(session).ifPresent(ms -> {
            if (ms.config().resolverResolverPostProcessorEnabled()) {
                for (ArtifactResult artifactResult : artifactResults) {
                    if (artifactResult.getRepository() instanceof RemoteRepository remoteRepository) {
                        Artifact artifact = artifactResult.getArtifact();
                        boolean resolved = artifactResult.isResolved();
                        if (resolved) {
                            try {
                                // store it; if needed
                                if (ms.mayStore(
                                        remoteRepository,
                                        artifact,
                                        artifact.getFile().toPath(),
                                        Collections.emptyMap())) {
                                    logger.debug("Stored artifact {} ({})", artifact, artifact.getFile());
                                }
                            } catch (IOException e) {
                                throw new UncheckedIOException(e);
                            }
                        }
                    }
                }
            }
        });
    }
}
