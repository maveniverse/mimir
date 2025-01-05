/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.shared.impl;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.mimir.shared.CacheKey;
import eu.maveniverse.maven.mimir.shared.Config;
import eu.maveniverse.maven.mimir.shared.naming.NameMapper;
import eu.maveniverse.maven.mimir.shared.naming.NameMapperFactory;
import java.io.IOException;
import java.util.Optional;
import javax.inject.Named;
import javax.inject.Singleton;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.repository.RemoteRepository;

@Singleton
@Named
public class NameMapperFactoryImpl implements NameMapperFactory {
    @Override
    public NameMapper createNameMapper(Config config) throws IOException {
        requireNonNull(config, "config");
        String mapper = "default";
        if (config.effectiveProperties().containsKey("mimir.mapper")) {
            mapper = config.effectiveProperties().get("mimir.mapper");
        }
        // TODO: mapper strategies selection
        return new DefaultNameMapper();
    }

    /**
     * This is SIMPLE name mapper, not quite usable (yet). More logic needed, like proper identification of remote
     * repositories, support repo aliases, mirrors, etc.
     * <p>
     * For this POC, only "central" and release artifacts are supported.
     */
    private static class DefaultNameMapper implements NameMapper {
        @Override
        public Optional<CacheKey> cacheKey(RemoteRepository remoteRepository, Artifact artifact) {
            if ("central".equals(remoteRepository.getId()) && !artifact.isSnapshot()) {
                String bucket = remoteRepository.getId();
                String name = artifact.getGroupId() + "/" + artifact.getArtifactId() + "/" + artifact.getVersion() + "/"
                        + artifact.getArtifactId() + "-" + artifact.getVersion();
                if (artifact.getClassifier() != null
                        && !artifact.getClassifier().trim().isEmpty()) {
                    name += "-" + artifact.getClassifier();
                }
                name += "." + artifact.getExtension();
                return Optional.of(CacheKey.of(bucket, name));
            } else {
                return Optional.empty();
            }
        }
    }
}
