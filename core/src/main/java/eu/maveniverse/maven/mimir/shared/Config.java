/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.shared;

import static eu.maveniverse.maven.mimir.shared.impl.Utils.toMap;
import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.mimir.shared.impl.Utils;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Simple Mimir configuration.
 */
public interface Config {
    String UNKNOWN = "n/a";

    String mimirVersion();

    Path basedir();

    Map<String, String> userProperties();

    Map<String, String> systemProperties();

    Map<String, String> effectiveProperties();

    default Builder toBuilder() {
        return new Builder(mimirVersion(), basedir(), userProperties(), systemProperties());
    }

    static Builder defaults() {
        return new Builder();
    }

    class Builder {
        private final String mimirVersion;
        private Path basedir;
        private Map<String, String> userProperties;
        private Map<String, String> systemProperties;

        private Builder() {
            this.mimirVersion = Utils.discoverArtifactVersion(
                    Config.class.getClassLoader(), "eu.maveniverse.maven.mimir", "core", UNKNOWN);
            Path userHome = discoverUserHomeDirectory();
            this.basedir = userHome.resolve(".mimir");
            this.userProperties = new HashMap<>();
            this.systemProperties = toMap(System.getProperties());
        }

        private Builder(
                String mimirVersion,
                Path basedir,
                Map<String, String> userProperties,
                Map<String, String> systemProperties) {
            this.mimirVersion = mimirVersion;
            this.basedir = basedir;
            this.userProperties = userProperties;
            this.systemProperties = systemProperties;
        }

        public Builder basedir(Path basedir) {
            this.basedir = getCanonicalPath(basedir);
            return this;
        }

        public Builder userProperties(Map<String, String> userProperties) {
            this.userProperties = new HashMap<>(userProperties);
            return this;
        }

        public Builder systemProperties(Map<String, String> systemProperties) {
            this.systemProperties = new HashMap<>(systemProperties);
            return this;
        }

        public Config build() {
            return new Impl(mimirVersion, basedir, userProperties, systemProperties);
        }

        private static class Impl implements Config {
            private final String mimirVersion;
            private final Path basedir;
            private final Map<String, String> userProperties;
            private final Map<String, String> systemProperties;
            private final Map<String, String> effectiveProperties;

            private Impl(
                    String mimirVersion,
                    Path basedir,
                    Map<String, String> userProperties,
                    Map<String, String> systemProperties) {
                this.mimirVersion = requireNonNull(mimirVersion, "mimirVersion");
                this.basedir = requireNonNull(basedir, "basedir");

                Properties mimirProperties = new Properties();
                Path mimirPropertiesPath = basedir.resolve("mimir.properties");
                if (Files.isRegularFile(mimirPropertiesPath)) {
                    try (InputStream inputStream = Files.newInputStream(mimirPropertiesPath)) {
                        mimirProperties.load(inputStream);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                }

                this.userProperties = Collections.unmodifiableMap(requireNonNull(userProperties, "userProperties"));
                this.systemProperties =
                        Collections.unmodifiableMap(requireNonNull(systemProperties, "systemProperties"));
                HashMap<String, String> eff = new HashMap<>();
                eff.putAll(systemProperties);
                eff.putAll(toMap(mimirProperties));
                eff.putAll(userProperties);
                this.effectiveProperties = Collections.unmodifiableMap(eff);
            }

            @Override
            public String mimirVersion() {
                return mimirVersion;
            }

            @Override
            public Path basedir() {
                return basedir;
            }

            @Override
            public Map<String, String> userProperties() {
                return userProperties;
            }

            @Override
            public Map<String, String> systemProperties() {
                return systemProperties;
            }

            @Override
            public Map<String, String> effectiveProperties() {
                return effectiveProperties;
            }
        }
    }

    static Path discoverUserHomeDirectory() {
        String userHome = System.getProperty("user.home");
        if (userHome == null) {
            throw new IllegalStateException("requires user.home Java System Property set");
        }
        return getCanonicalPath(Paths.get(userHome));
    }

    static Path getCanonicalPath(Path path) {
        requireNonNull(path, "path");
        try {
            return path.toRealPath();
        } catch (IOException e) {
            return getCanonicalPath(path.getParent()).resolve(path.getFileName());
        }
    }
}
