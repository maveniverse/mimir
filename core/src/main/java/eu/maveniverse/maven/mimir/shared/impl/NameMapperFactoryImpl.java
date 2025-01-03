/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.shared.impl;

import eu.maveniverse.maven.mimir.shared.CacheKey;
import eu.maveniverse.maven.mimir.shared.naming.NameMapper;
import eu.maveniverse.maven.mimir.shared.naming.NameMapperFactory;
import java.io.IOException;
import java.util.Map;
import javax.inject.Named;
import javax.inject.Singleton;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.repository.RemoteRepository;

@Singleton
@Named
public class NameMapperFactoryImpl implements NameMapperFactory {
    @Override
    public NameMapper createNameMapper(Map<String, Object> config) throws IOException {
        // TODO:
        return new SimpleNameMapper();
    }

    private static class SimpleNameMapper implements NameMapper {
        @Override
        public CacheKey cacheKey(RemoteRepository remoteRepository, Artifact artifact) {
            String bucket = remoteRepository.getId();
            String name = artifact.getGroupId() + "/" + artifact.getArtifactId() + "/" + artifact.getVersion() + "/"
                    + artifact.getArtifactId() + "-" + artifact.getVersion();
            if (artifact.getClassifier() != null
                    && !artifact.getClassifier().trim().isEmpty()) {
                name += "-" + artifact.getClassifier();
            }
            name += "." + artifact.getExtension();
            return CacheKey.of(bucket, name);
        }
    }
}
