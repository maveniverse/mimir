/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.shared.naming;

import eu.maveniverse.maven.mimir.shared.CacheKey;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.repository.RemoteRepository;

public interface NameMapper {
    /**
     * Creates a cache key according to naming strategy.
     */
    CacheKey cacheKey(RemoteRepository remoteRepository, Artifact artifact);
}
