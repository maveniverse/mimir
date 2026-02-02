/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.shared.naming;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.mimir.shared.impl.naming.Artifacts;
import java.net.URI;
import java.util.Optional;
import java.util.function.Function;
import org.eclipse.aether.artifact.Artifact;

/**
 * Builds from {@link URI} instances various keys.
 */
public final class Keys {
    private Keys() {}

    public abstract static class Key {
        protected final URI uri;

        protected Key(URI uri) {
            this.uri = requireNonNull(uri);
        }

        public URI toUri() {
            return uri;
        }

        @Override
        public String toString() {
            return uri.toASCIIString();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj instanceof Key other) {
                return this.uri.equals(other.uri);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return uri.hashCode();
        }
    }

    public static final class FileKey extends Key {
        private final String container;
        private final String path;

        public FileKey(URI uri, String container, String path) {
            super(uri);
            this.container = requireNonNull(container);
            this.path = requireNonNull(path);
        }

        public String container() {
            return container;
        }

        public String path() {
            return path;
        }
    }

    public static final class ArtifactKey extends Key {
        private final String container;
        private final Artifact artifact;

        public ArtifactKey(URI uri, String container, Artifact artifact) {
            super(uri);
            this.container = requireNonNull(container);
            this.artifact = requireNonNull(artifact);
        }

        public String container() {
            return container;
        }

        public Artifact artifact() {
            return artifact;
        }
    }

    public static final class CasKey extends Key {
        private final String type;
        private final String address;

        public CasKey(URI uri, String type, String address) {
            super(uri);
            this.type = requireNonNull(type);
            this.address = requireNonNull(address);
        }

        public String type() {
            return type;
        }

        public String address() {
            return address;
        }
    }

    public static Optional<FileKey> mayMapToFileKey(Keys.Key key) {
        if (key instanceof FileKey fk) {
            return Optional.of(fk);
        }
        if (key instanceof ArtifactKey ak) {
            return Optional.of(convertToFileKey(ak, Artifacts::artifactRepositoryPath));
        }
        return Optional.empty();
    }

    private static FileKey convertToFileKey(ArtifactKey artifactKey, Function<Artifact, String> layout) {
        requireNonNull(artifactKey);
        requireNonNull(layout);
        String path = layout.apply(artifactKey.artifact());
        return new FileKey(UriEncoders.fileKeyBuilder(artifactKey.container(), path), artifactKey.container(), path);
    }
}
