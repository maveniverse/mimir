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
import eu.maveniverse.maven.mimir.shared.naming.Key;
import eu.maveniverse.maven.mimir.shared.naming.KeyResolver;
import eu.maveniverse.maven.mimir.shared.naming.KeyResolverFactory;
import java.net.URI;
import java.util.function.Function;
import javax.inject.Named;
import javax.inject.Singleton;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;

@Singleton
@Named(SimpleKeyResolverFactory.NAME)
public final class SimpleKeyResolverFactory implements KeyResolverFactory {
    public static final String NAME = "simple";

    @Override
    public KeyResolver createKeyResolver(SessionConfig sessionConfig) {
        requireNonNull(sessionConfig);
        return new SimpleKeyResolver(SimpleKeyResolverFactory::artifactPath);
    }

    /**
     * Provides "path" for artifact.
     */
    public static String artifactPath(Artifact artifact) {
        String name = artifact.getGroupId() + "/" + artifact.getArtifactId() + "/" + artifact.getBaseVersion() + "/"
                + artifact.getArtifactId() + "-" + artifact.getVersion();
        if (artifact.getClassifier() != null && !artifact.getClassifier().trim().isEmpty()) {
            name += "-" + artifact.getClassifier();
        }
        name += "." + artifact.getExtension();
        return name;
    }

    /**
     * Provides "path" for artifact.
     */
    public static String artifactRepositoryPath(Artifact artifact) {
        String name = artifact.getGroupId().replaceAll("\\.", "/") + "/" + artifact.getArtifactId() + "/"
                + artifact.getBaseVersion() + "/" + artifact.getArtifactId() + "-" + artifact.getVersion();
        if (artifact.getClassifier() != null && !artifact.getClassifier().trim().isEmpty()) {
            name += "-" + artifact.getClassifier();
        }
        name += "." + artifact.getExtension();
        return name;
    }

    /**
     * This is SIMPLE key resolver; fully usable for any standard scenario.
     * <p>
     * Note: the layout this name mapper uses is intentionally non-standard, and is selected on purpose: to discourage
     * any direct tampering with cache contents. In essence, same rules applies as are in effect for Maven local repository:
     * no direct tampering. The layout should be considered "internal" and may change without any compatibility obligation.
     */
    public static class SimpleKeyResolver implements KeyResolver {
        private final Function<Artifact, String> artifactPathMapper;

        public SimpleKeyResolver(Function<Artifact, String> artifactPathMapper) {
            this.artifactPathMapper = artifactPathMapper;
        }

        /**
         * Resolves a cache key according to naming strategy.
         */
        @Override
        public Key apply(URI key) {
            if (key.isOpaque()) {
                if ("mimir".equals(key.getScheme())) {
                    String ssp = key.getSchemeSpecificPart();
                    if (ssp.startsWith("artifact:")) {
                        String[] bits = ssp.substring(9).split(":", 2);
                        if (bits.length == 2) {
                            String container = bits[0];
                            Artifact artifact = new DefaultArtifact(bits[1]);
                            return Key.of(container, artifactPathMapper.apply(artifact));
                        }
                    } else if (ssp.startsWith("file:")) {
                        String[] bits = ssp.substring(5).split(":", 2);
                        if (bits.length == 2) {
                            String container = bits[0];
                            String path = bits[1];
                            return Key.of(container, path);
                        }
                    }
                }
            }
            throw new IllegalArgumentException("Unexpected URI");
        }
    }
}
