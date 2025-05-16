/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.shared;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.shared.core.maven.MavenUtils;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;

/**
 * Simple Mimir configuration.
 */
public interface Config {
    String NAME = "mimir";

    String CONF_PREFIX = NAME + ".";

    /**
     * Mimir "local host hint": on hosts where there is running Docker, Tailscale etc. it may be impossible to "figure out"
     * which interface and which address correspond to LAN address, if any. Hence, one can give Mimir a "hint" that is
     * globally applied at Mimir level (like publishers or LAN sharing is). Accepted hints are:
     *
     * <ul>
     *     <li><code>match-interface:value</code></li>
     *     <li><code>match-address:value</code></li>
     * </ul>
     *
     * In both cases "value" may end with "*" (asterisk). If no asterisk, value equality is checked, if it ends with
     * asterisk, then "starts with value" is checked.
     * Examples:
     *
     * <ul>
     *     <li><code>match-interface:enp*</code> means "use interface whose name starts with enp"</li>
     *     <li><code>match-address:192.168.1.10</code> means "use address that equals to 192.168.1.10"</li>
     * </ul>
     *
     * These hints may still produce wrong address selection, so one should check logs to ensure about them.
     */
    String CONF_LOCAL_HOST_HINT = CONF_PREFIX + "localHostHint";

    boolean enabled();

    Optional<String> mimirVersion();

    Path basedir();

    Path propertiesPath();

    Map<String, String> userProperties();

    Map<String, String> systemProperties();

    Map<String, String> effectiveProperties();

    default Optional<String> localHostHint() {
        return Optional.ofNullable(effectiveProperties().get(CONF_LOCAL_HOST_HINT));
    }

    default Builder toBuilder() {
        return new Builder(
                enabled(),
                mimirVersion().orElse(null),
                basedir(),
                propertiesPath(),
                userProperties(),
                systemProperties());
    }

    static Builder defaults() {
        return new Builder(
                null,
                MavenUtils.discoverArtifactVersion(
                        Config.class.getClassLoader(), "eu.maveniverse.maven.mimir", "core", null),
                discoverBaseDirectory(),
                Path.of("mimir.properties"),
                new HashMap<>(),
                MavenUtils.toMap(System.getProperties()));
    }

    static Builder daemonDefaults() {
        return defaults().propertiesPath(Path.of("daemon.properties"));
    }

    class Builder {
        private Boolean enabled;
        private final String mimirVersion;
        private Path basedir;
        private Path propertiesPath;
        private Map<String, String> userProperties;
        private Map<String, String> systemProperties;

        private Builder(
                Boolean enabled,
                String mimirVersion,
                Path basedir,
                Path propertiesPath,
                Map<String, String> userProperties,
                Map<String, String> systemProperties) {
            this.enabled = enabled;
            this.mimirVersion = mimirVersion;
            this.basedir = basedir;
            this.propertiesPath = propertiesPath;
            this.userProperties = new HashMap<>(userProperties);
            this.systemProperties = new HashMap<>(systemProperties);
        }

        public Builder enabled(Boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public Builder basedir(Path basedir) {
            this.basedir = getCanonicalPath(basedir);
            return this;
        }

        public Builder propertiesPath(Path propertiesPath) {
            this.propertiesPath = requireNonNull(propertiesPath, "propertiesPath");
            return this;
        }

        public Builder userProperties(Map<String, String> userProperties) {
            this.userProperties = new HashMap<>(userProperties);
            return this;
        }

        public Builder setUserProperty(String key, String value) {
            requireNonNull(key, "key");
            requireNonNull(value, "value");
            this.userProperties.put(key, value);
            return this;
        }

        public Builder systemProperties(Map<String, String> systemProperties) {
            this.systemProperties = new HashMap<>(systemProperties);
            return this;
        }

        public Builder setSystemProperty(String key, String value) {
            requireNonNull(key, "key");
            requireNonNull(value, "value");
            this.systemProperties.put(key, value);
            return this;
        }

        public Config build() {
            return new Impl(enabled, mimirVersion, basedir, propertiesPath, userProperties, systemProperties);
        }

        private static class Impl implements Config {
            private final Boolean enabled;
            private final String mimirVersion;
            private final Path basedir;
            private final Path propertiesPath;
            private final Map<String, String> userProperties;
            private final Map<String, String> systemProperties;
            private final Map<String, String> effectiveProperties;

            private Impl(
                    Boolean enabled,
                    String mimirVersion,
                    Path basedir,
                    Path propertiesPath,
                    Map<String, String> userProperties,
                    Map<String, String> systemProperties) {
                this.enabled = enabled;
                this.mimirVersion = mimirVersion;
                this.basedir = requireNonNull(basedir, "basedir");
                requireNonNull(propertiesPath, "propertiesPath");
                this.propertiesPath = getCanonicalPath(basedir.resolve(propertiesPath));

                Properties mimirProperties = new Properties();
                if (Files.isRegularFile(this.propertiesPath)) {
                    try (InputStream inputStream = Files.newInputStream(this.propertiesPath)) {
                        mimirProperties.load(inputStream);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                }

                this.userProperties = Map.copyOf(requireNonNull(userProperties, "userProperties"));
                this.systemProperties = Map.copyOf(requireNonNull(systemProperties, "systemProperties"));
                HashMap<String, String> eff = new HashMap<>();
                eff.putAll(systemProperties);
                eff.putAll(MavenUtils.toMap(mimirProperties));
                eff.putAll(userProperties);
                this.effectiveProperties = Map.copyOf(eff);
            }

            @Override
            public boolean enabled() {
                return Objects.requireNonNullElseGet(
                        enabled,
                        () -> Boolean.parseBoolean(
                                effectiveProperties.getOrDefault("mimir.enabled", Boolean.TRUE.toString())));
            }

            @Override
            public Optional<String> mimirVersion() {
                return Optional.ofNullable(mimirVersion);
            }

            @Override
            public Path basedir() {
                return basedir;
            }

            @Override
            public Path propertiesPath() {
                return propertiesPath;
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

    static Path discoverBaseDirectory() {
        String basedir = System.getProperty("mimir.basedir");
        if (basedir == null) {
            return getCanonicalPath(discoverUserHomeDirectory().resolve(".mimir"));
        }
        return getCanonicalPath(Path.of(System.getProperty("user.dir")).resolve(basedir));
    }

    static Path discoverUserHomeDirectory() {
        String userHome = System.getProperty("user.home");
        if (userHome == null) {
            throw new IllegalStateException("requires user.home Java System Property set");
        }
        return getCanonicalPath(Path.of(userHome));
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
