/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.shared.impl.naming;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.mimir.shared.Config;
import eu.maveniverse.maven.mimir.shared.naming.KeyMapper;
import eu.maveniverse.maven.mimir.shared.naming.KeyMapperFactory;
import java.net.URI;
import javax.inject.Named;
import javax.inject.Singleton;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.util.artifact.ArtifactIdUtils;

@Singleton
@Named(SimpleKeyMapperFactory.NAME)
public final class SimpleKeyMapperFactory implements KeyMapperFactory {
    public static final String NAME = "simple";

    @Override
    public KeyMapper createKeyMapper(Config config) {
        requireNonNull(config, "config");
        return new SimpleKeyMapper();
    }

    /**
     * This is SIMPLE name mapper; fully usable for any standard scenario.
     * <p>
     * More logic may be needed for more complex scenarios, like proper identification of remote repositories,
     * support repo aliases, mirrors, etc.
     */
    public static class SimpleKeyMapper implements KeyMapper {
        /**
         * Provides default and simplistic "container" implementation.
         */
        public String container(RemoteRepository repository) {
            return repository.getId();
        }

        /**
         * Provides default and simplistic "name" implementation.
         */
        public String name(Artifact artifact) {
            return ArtifactIdUtils.toId(artifact);
        }

        /**
         * Creates a cache key according to naming strategy.
         */
        @Override
        public URI apply(RemoteRepository remoteRepository, Artifact artifact) {
            return URI.create("mimir:artifact:" + container(remoteRepository) + ":" + name(artifact));
        }
    }
}
