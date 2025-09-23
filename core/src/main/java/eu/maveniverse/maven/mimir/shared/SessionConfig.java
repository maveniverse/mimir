/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.shared;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.shared.core.fs.FileUtils;
import eu.maveniverse.maven.shared.core.maven.MavenUtils;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import org.eclipse.aether.RepositorySystemSession;

/**
 * Simple Mimir configuration.
 * <p>
 * Note: to "relocate" Mimir basedir, use {@code mimir.basedir} Java System Property with value containing a path of
 * alternate basedir.
 */
public interface SessionConfig {
    String NAME = "mimir";

    String UNKNOWN_VERSION = "unknown";

    String CONF_PREFIX = NAME + ".";

    /**
     * Enables/disables Mimir. Defaults to {@code true}.
     */
    String CONF_ENABLED = CONF_PREFIX + "enabled";

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

    /**
     * Makes Mimir ignore (not fail the build) if session close at Maven session end fails.
     * Note: use of this configuration is <em>discouraged</em>. Defaults to {@code false}.
     */
    String CONF_IGNORE_ERROR_AT_SESSION_END = CONF_PREFIX + "ignoreErrorAtSessionEnd";

    boolean enabled();

    boolean ignoreErrorAtSessionEnd();

    String mimirVersion();

    Path basedir();

    Path propertiesPath();

    Map<String, String> userProperties();

    Map<String, String> systemProperties();

    Map<String, String> effectiveProperties();

    Optional<String> localHostHint();

    Optional<RepositorySystemSession> repositorySystemSession();

    default Builder toBuilder() {
        return new Builder(
                enabled(),
                ignoreErrorAtSessionEnd(),
                mimirVersion(),
                basedir(),
                propertiesPath(),
                userProperties(),
                systemProperties(),
                repositorySystemSession().orElse(null));
    }

    static Builder defaults() {
        return new Builder(
                null,
                null,
                MavenUtils.discoverArtifactVersion(
                        SessionConfig.class.getClassLoader(), "eu.maveniverse.maven.mimir", "core", UNKNOWN_VERSION),
                null,
                null,
                Collections.emptyMap(),
                MavenUtils.toMap(System.getProperties()),
                null);
    }

    static Builder daemonDefaults() {
        return defaults().propertiesPath(Path.of("daemon.properties"));
    }

    class Builder {
        private Boolean enabled;
        private Boolean ignoreErrorAtSessionEnd;
        private final String mimirVersion;
        private Path basedir;
        private Path propertiesPath;
        private Map<String, String> userProperties;
        private Map<String, String> systemProperties;
        private RepositorySystemSession repositorySystemSession;

        private Builder(
                Boolean enabled,
                Boolean ignoreErrorAtSessionEnd,
                String mimirVersion,
                Path basedir,
                Path propertiesPath,
                Map<String, String> userProperties,
                Map<String, String> systemProperties,
                RepositorySystemSession repositorySystemSession) {
            this.enabled = enabled;
            this.ignoreErrorAtSessionEnd = ignoreErrorAtSessionEnd;
            this.mimirVersion = requireNonNull(mimirVersion);
            this.basedir = basedir;
            this.propertiesPath = propertiesPath;
            this.userProperties = new HashMap<>(userProperties);
            this.systemProperties = new HashMap<>(systemProperties);
            this.repositorySystemSession = repositorySystemSession;
        }

        public Builder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public Builder ignoreErrorAtSessionEnd(boolean ignoreErrorAtSessionEnd) {
            this.ignoreErrorAtSessionEnd = ignoreErrorAtSessionEnd;
            return this;
        }

        public Builder basedir(Path basedir) {
            this.basedir = FileUtils.canonicalPath(basedir);
            return this;
        }

        public Builder propertiesPath(Path propertiesPath) {
            this.propertiesPath = requireNonNull(propertiesPath, "propertiesPath");
            return this;
        }

        public Builder userProperties(Map<String, String> userProperties) {
            requireNonNull(userProperties, "userProperties");
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
            requireNonNull(systemProperties, "systemProperties");
            this.systemProperties = new HashMap<>(systemProperties);
            return this;
        }

        public Builder setSystemProperty(String key, String value) {
            requireNonNull(key, "key");
            requireNonNull(value, "value");
            this.systemProperties.put(key, value);
            return this;
        }

        public Builder repositorySystemSession(RepositorySystemSession repositorySystemSession) {
            this.repositorySystemSession = repositorySystemSession;
            return this;
        }

        public SessionConfig build() {
            return new Impl(
                    enabled,
                    ignoreErrorAtSessionEnd,
                    mimirVersion,
                    basedir,
                    propertiesPath,
                    userProperties,
                    systemProperties,
                    repositorySystemSession);
        }

        private static class Impl implements SessionConfig {
            private final boolean enabled;
            private final boolean ignoreErrorAtSessionEnd;
            private final String mimirVersion;
            private final Path basedir;
            private final Path propertiesPath;
            private final Map<String, String> userProperties;
            private final Map<String, String> systemProperties;
            private final Map<String, String> effectiveProperties;
            private final String localHostHint;
            private final RepositorySystemSession repositorySystemSession;

            private Impl(
                    Boolean enabled,
                    Boolean ignoreErrorAtSessionEnd,
                    String mimirVersion,
                    Path basedir,
                    Path propertiesPath,
                    Map<String, String> userProperties,
                    Map<String, String> systemProperties,
                    RepositorySystemSession repositorySystemSession) {
                this.mimirVersion = requireNonNull(mimirVersion, "mimirVersion");

                this.basedir = basedir == null
                        ? FileUtils.discoverBaseDirectory("mimir.basedir", ".mimir")
                        : FileUtils.canonicalPath(basedir);
                this.propertiesPath = propertiesPath == null
                        ? this.basedir.resolve("session.properties")
                        : FileUtils.canonicalPath(this.basedir.resolve(propertiesPath));

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

                this.enabled =
                        Boolean.parseBoolean(effectiveProperties.getOrDefault(CONF_ENABLED, Boolean.TRUE.toString()));
                this.ignoreErrorAtSessionEnd = Boolean.parseBoolean(
                        effectiveProperties.getOrDefault(CONF_IGNORE_ERROR_AT_SESSION_END, Boolean.FALSE.toString()));

                this.localHostHint = effectiveProperties.get(CONF_LOCAL_HOST_HINT);
                this.repositorySystemSession = repositorySystemSession;
            }

            @Override
            public boolean enabled() {
                return enabled;
            }

            public boolean ignoreErrorAtSessionEnd() {
                return ignoreErrorAtSessionEnd;
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

            @Override
            public Optional<String> localHostHint() {
                return Optional.ofNullable(localHostHint);
            }

            @Override
            public Optional<RepositorySystemSession> repositorySystemSession() {
                return Optional.ofNullable(repositorySystemSession);
            }
        }
    }
}
