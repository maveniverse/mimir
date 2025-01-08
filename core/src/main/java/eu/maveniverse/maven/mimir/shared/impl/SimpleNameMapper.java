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
import eu.maveniverse.maven.mimir.shared.naming.NameMapper;
import java.util.Optional;
import java.util.function.Predicate;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.repository.RemoteRepository;

/**
 * This is SIMPLE name mapper; fully usable for any standard scenario: it handles release artifacts ONLY.
 * The "supported repository" is handled by ctor injected predicate.
 * More logic may be needed for more complex scenarios, like proper identification of remote repositories,
 * support repo aliases, mirrors, etc.
 * <p>
 * Note: the layout this name mapper uses is intentionally non-standard, and is selected on purpose: to discourage
 * any direct tampering with cache contents. In essence, same rules applies as are in effect for Maven local repository:
 * no direct tampering. The layout should be considered "internal" and may change without any compatibility obligation.
 */
public class SimpleNameMapper implements NameMapper {
    private final Predicate<RemoteRepository> remoteRepositoryPredicate;

    public SimpleNameMapper(Predicate<RemoteRepository> remoteRepositoryPredicate) {
        this.remoteRepositoryPredicate = requireNonNull(remoteRepositoryPredicate, "remoteRepositoryPredicate");
    }

    @Override
    public boolean supports(RemoteRepository remoteRepository) {
        return remoteRepositoryPredicate.test(remoteRepository);
    }

    @Override
    public Optional<CacheKey> cacheKey(RemoteRepository remoteRepository, Artifact artifact) {
        if (remoteRepositoryPredicate.test(remoteRepository) && !artifact.isSnapshot()) {
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
