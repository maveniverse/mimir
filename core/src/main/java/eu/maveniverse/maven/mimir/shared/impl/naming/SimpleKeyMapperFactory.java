/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.shared.impl.naming;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.mimir.shared.SessionConfig;
import eu.maveniverse.maven.mimir.shared.naming.KeyMapper;
import eu.maveniverse.maven.mimir.shared.naming.KeyMapperFactory;
import eu.maveniverse.maven.mimir.shared.naming.RemoteRepositories;
import java.net.URI;
import java.util.function.Predicate;
import javax.inject.Named;
import javax.inject.Singleton;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.util.StringDigestUtil;
import org.eclipse.aether.util.artifact.ArtifactIdUtils;

@Singleton
@Named(SimpleKeyMapperFactory.NAME)
public final class SimpleKeyMapperFactory implements KeyMapperFactory {
    public static final String NAME = "simple";

    @Override
    public KeyMapper createKeyMapper(SessionConfig sessionConfig) {
        requireNonNull(sessionConfig);
        return new SimpleKeyMapper();
    }

    private static final Predicate<RemoteRepository> CENTRAL_PREDICATE = RemoteRepositories.centralDirectOnly();

    /**
     * Provides default and simplistic "container" implementation.
     */
    public static String container(RemoteRepository repository) {
        if (CENTRAL_PREDICATE.test(repository)) {
            return repository.getId();
        } else {
            return repository.getId() + "-" + StringDigestUtil.sha1(repository.getUrl());
        }
    }

    /**
     * Provides default and simplistic "name" implementation.
     */
    public static String name(Artifact artifact) {
        return ArtifactIdUtils.toId(artifact);
    }

    /**
     * This is SIMPLE name mapper; fully usable for any standard scenario.
     * <p>
     * More logic may be needed for more complex scenarios, like proper identification of remote repositories,
     * support repo aliases, mirrors, etc.
     */
    public static class SimpleKeyMapper implements KeyMapper {
        /**
         * Creates a cache key according to naming strategy.
         */
        @Override
        public URI apply(RemoteRepository remoteRepository, Artifact artifact) {
            return URI.create("mimir:artifact:" + container(remoteRepository) + ":" + name(artifact));
        }
    }
}
