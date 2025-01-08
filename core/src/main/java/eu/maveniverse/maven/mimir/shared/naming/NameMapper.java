/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.shared.naming;

import eu.maveniverse.maven.mimir.shared.CacheKey;
import java.util.Optional;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.repository.RemoteRepository;

public interface NameMapper {
    /**
     * A name mapper that is nop.
     */
    NameMapper NOP = new NameMapper() {
        @Override
        public boolean supports(RemoteRepository remoteRepository) {
            return false;
        }

        @Override
        public Optional<CacheKey> cacheKey(RemoteRepository remoteRepository, Artifact artifact) {
            return Optional.empty();
        }
    };

    /**
     * Returns {@code true} if the given remote repository is supported by this mapper.
     */
    boolean supports(RemoteRepository remoteRepository);

    /**
     * Creates a cache key according to naming strategy, if supported. If no cache key creation possible, Mimir
     * will step aside for given transaction.
     */
    Optional<CacheKey> cacheKey(RemoteRepository remoteRepository, Artifact artifact);
}
