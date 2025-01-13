/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.shared.impl;

import eu.maveniverse.maven.mimir.shared.naming.CacheKey;
import eu.maveniverse.maven.mimir.shared.naming.NameMapper;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.repository.RemoteRepository;

/**
 * This is SIMPLE name mapper; fully usable for any standard scenario.
 * More logic may be needed for more complex scenarios, like proper identification of remote repositories,
 * support repo aliases, mirrors, etc.
 * <p>
 * Note: the layout this name mapper uses is intentionally non-standard, and is selected on purpose: to discourage
 * any direct tampering with cache contents. In essence, same rules applies as are in effect for Maven local repository:
 * no direct tampering. The layout should be considered "internal" and may change without any compatibility obligation.
 */
public class SimpleNameMapper implements NameMapper {
    @Override
    public CacheKey cacheKey(RemoteRepository remoteRepository, Artifact artifact) {
        String container = remoteRepository.getId();
        String name = artifact.getGroupId() + "/" + artifact.getArtifactId() + "/" + artifact.getBaseVersion() + "/"
                + artifact.getArtifactId() + "-" + artifact.getVersion();
        if (artifact.getClassifier() != null && !artifact.getClassifier().trim().isEmpty()) {
            name += "-" + artifact.getClassifier();
        }
        name += "." + artifact.getExtension();
        return CacheKey.of(container, name);
    }
}
