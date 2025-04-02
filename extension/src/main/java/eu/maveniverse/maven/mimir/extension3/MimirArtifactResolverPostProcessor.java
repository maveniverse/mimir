/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.extension3;

import java.util.List;
import javax.inject.Named;
import javax.inject.Singleton;
import org.eclipse.aether.RepositorySystemSession;
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
            logger.info(
                    "{} @ {} < {}",
                    artifactResult.getArtifact(),
                    artifactResult.getRequest().getRequestContext(),
                    artifactResult.getRepository().getId());
        }
    }
}
