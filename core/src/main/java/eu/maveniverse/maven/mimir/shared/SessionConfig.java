/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.shared;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.mimir.shared.impl.naming.SimpleKeyMapperFactory;
import eu.maveniverse.maven.mimir.shared.naming.RemoteRepositories;
import eu.maveniverse.maven.mimir.shared.node.LocalNode;
import eu.maveniverse.maven.shared.core.fs.FileUtils;
import eu.maveniverse.maven.shared.core.maven.MavenUtils;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
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

    Path baseLocksDir();

    Path propertiesPath();

    Map<String, String> userProperties();

    Map<String, String> systemProperties();

    Map<String, String> effectiveProperties();

    Optional<String> localHostHint();

    Optional<RepositorySystemSession> repositorySystemSession();

    // session impl config

    /**
     * The key mapper that session should use.
     * <p>
     * Configuration key {@code mimir.session.keyMapper}
     * <p>
     * By default, is "simple".
     */
    String keyMapper();

    /**
     * The overlay nodes.
     * <p>
     * Configuration key {@code mimir.session.overlayNodes}
     * <p>
     * By default, is empty. One can give here overlay local nodes that will be overlaid onto main local node.
     */
    Set<String> overlayNodes();

    /**
     * The local node name, that session should use.
     * <p>
     * Configuration key {@code mimir.session.localNode}
     * <p>
     * By default, is "daemon". Accepts any string representing node name.
     */
    String localNode();

    /**
     * Pre-existing instance, if given. Otherwise, session will create and manage one.
     */
    Optional<LocalNode> localNodeInstance();

    /**
     * The repositories that Mimir should cache. Note: Mimir handles ONLY release artifacts, which are immutable. If
     * your workflow contains mutable release artifacts, do NOT use Mimir.
     * <p>
     * Configuration key {@code mimir.session.repositories}
     * <p>
     * By default, is "central(directOnly,releaseOnly,httpsOnly)". Hence, Maven Central direct (non-mirrored) will be
     * cached ONLY. The configuration may contain comma separated list of repository IDs (with modifiers) Mimir should
     * manage, or {@code *} as "any". In this case, Mimir will handle any release repository it meets. The asterisk
     * may also have modifiers so {@code *(releaseOnly;httpsOnly)} means any HTTPS repository.
     * <p>
     * The modifiers may appear in braces without any whitespace after repository ID or {@code *} and may be:
     * <ul>
     *     <li>{@code directOnly} remote repository is not subject of {@code mirrorOf}</li>
     *     <li>{@code releaseOnly} remote repository has only RELEASE policy enabled (not snapshot or both)</li>
     *     <li>{@code httpsOnly} remote repository protocol is HTTPS only (case-insensitive)</li>
     * </ul>
     * The modifiers are split with {@code ,} (comma).
     * <p>
     * The whole expression of comma separated repositories are parsed into list of predicates and joined with logical "or".
     * <p>
     * Warning for mirrors: usually in company environments, some public repositories may be redirected with
     * {@code mirrorOf}.
     */
    Set<String> repositories();

    /**
     * Returns the defined mirrors.
     */
    Set<String> mirrors();

    // components on/off

    boolean resolverConnectorEnabled();

    boolean resolverResolverPostProcessorEnabled();

    boolean resolverTrustedChecksumsSourceEnabled();

    default Builder toBuilder() {
        return new Builder(
                enabled(),
                ignoreErrorAtSessionEnd(),
                mimirVersion(),
                basedir(),
                propertiesPath(),
                userProperties(),
                systemProperties(),
                localNodeInstance().orElse(null),
                repositorySystemSession().orElse(null));
    }

    static Builder defaults() {
        Path mimirBasedir = null;
        String envMimirBasedir = System.getenv("MIMIR_BASEDIR");
        if (envMimirBasedir != null) {
            mimirBasedir = pathOf(envMimirBasedir);
        }
        Path mimirSessionConfigPath = null;
        String envMimirSessionConfigPath = System.getenv("MIMIR_SESSION_CONFIG_PATH");
        if (envMimirSessionConfigPath != null) {
            mimirSessionConfigPath = pathOf(envMimirSessionConfigPath);
        }

        return new Builder(
                null,
                null,
                MavenUtils.discoverArtifactVersion(
                        SessionConfig.class.getClassLoader(), "eu.maveniverse.maven.mimir", "core", UNKNOWN_VERSION),
                mimirBasedir,
                mimirSessionConfigPath,
                Collections.emptyMap(),
                MavenUtils.toMap(System.getProperties()),
                null,
                null);
    }

    static Builder daemonDefaults() {
        Path mimirDaemonConfigPath = Path.of("daemon.properties");
        String envMimirDaemonConfigPath = System.getenv("MIMIR_DAEMON_CONFIG_PATH");
        if (envMimirDaemonConfigPath != null) {
            mimirDaemonConfigPath = pathOf(envMimirDaemonConfigPath);
        }

        return defaults().propertiesPath(mimirDaemonConfigPath);
    }

    private static Path pathOf(String path) {
        requireNonNull(path, "path");
        if ("~".equals(path)) {
            return Path.of(System.getProperty("user.home")).normalize();
        } else if (path.startsWith("~/")) {
            return Path.of(System.getProperty("user.home") + path.substring(1)).normalize();
        } else {
            return Path.of(path).normalize();
        }
    }

    class Builder {
        private Boolean enabled;
        private Boolean ignoreErrorAtSessionEnd;
        private final String mimirVersion;
        private Path basedir;
        private Path propertiesPath;
        private Map<String, String> userProperties;
        private Map<String, String> systemProperties;
        private LocalNode localNodeInstance;
        private RepositorySystemSession repositorySystemSession;
        private boolean resolverConnectorEnabled = true;
        private boolean resolverResolverPostProcessorEnabled = true;
        private boolean resolverTrustedChecksumsSourceEnabled = true;

        private Builder(
                Boolean enabled,
                Boolean ignoreErrorAtSessionEnd,
                String mimirVersion,
                Path basedir,
                Path propertiesPath,
                Map<String, String> userProperties,
                Map<String, String> systemProperties,
                LocalNode localNodeInstance,
                RepositorySystemSession repositorySystemSession) {
            this.enabled = enabled;
            this.ignoreErrorAtSessionEnd = ignoreErrorAtSessionEnd;
            this.mimirVersion = requireNonNull(mimirVersion);
            this.basedir = basedir;
            this.propertiesPath = propertiesPath;
            this.userProperties = new HashMap<>(userProperties);
            this.systemProperties = new HashMap<>(systemProperties);
            this.localNodeInstance = localNodeInstance;
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

        public Builder localNodeInstance(LocalNode localNodeInstance) {
            this.localNodeInstance = localNodeInstance;
            return this;
        }

        public Builder repositorySystemSession(RepositorySystemSession repositorySystemSession) {
            this.repositorySystemSession = repositorySystemSession;
            return this;
        }

        public Builder resolverConnectorEnabled(boolean resolverConnectorEnabled) {
            this.resolverConnectorEnabled = resolverConnectorEnabled;
            return this;
        }

        public Builder resolverResolverPostProcessorEnabled(boolean resolverResolverPostProcessorEnabled) {
            this.resolverResolverPostProcessorEnabled = resolverResolverPostProcessorEnabled;
            return this;
        }

        public Builder resolverTrustedChecksumsSourceEnabled(boolean resolverTrustedChecksumsSourceEnabled) {
            this.resolverTrustedChecksumsSourceEnabled = resolverTrustedChecksumsSourceEnabled;
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
                    localNodeInstance,
                    repositorySystemSession,
                    resolverConnectorEnabled,
                    resolverResolverPostProcessorEnabled,
                    resolverTrustedChecksumsSourceEnabled);
        }

        private static class Impl implements SessionConfig {
            private final boolean enabled;
            private final boolean ignoreErrorAtSessionEnd;
            private final String mimirVersion;
            private final Path basedir;
            private final Path baseLocksDir;
            private final Path propertiesPath;
            private final Map<String, String> userProperties;
            private final Map<String, String> systemProperties;
            private final Map<String, String> effectiveProperties;
            private final String localHostHint;
            private final RepositorySystemSession repositorySystemSession;

            private final boolean resolverConnectorEnabled;
            private final boolean resolverResolverPostProcessorEnabled;
            private final boolean resolverTrustedChecksumsSourceEnabled;

            // session impl config (derived from that above)
            private final String keyMapper;
            private final Set<String> overlayNodes;
            private final String localNode;
            private final LocalNode localNodeInstance;
            private final Set<String> repositories;
            private final Set<String> mirrors;

            private Impl(
                    Boolean enabled,
                    Boolean ignoreErrorAtSessionEnd,
                    String mimirVersion,
                    Path basedir,
                    Path propertiesPath,
                    Map<String, String> userProperties,
                    Map<String, String> systemProperties,
                    LocalNode localNodeInstance,
                    RepositorySystemSession repositorySystemSession,
                    boolean resolverConnectorEnabled,
                    boolean resolverResolverPostProcessorEnabled,
                    boolean resolverTrustedChecksumsSourceEnabled) {
                this.mimirVersion = requireNonNull(mimirVersion, "mimirVersion");

                this.basedir = basedir == null
                        ? FileUtils.discoverBaseDirectory("mimir.basedir", ".mimir")
                        : FileUtils.canonicalPath(basedir);
                this.baseLocksDir = this.basedir.resolve("locks");
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

                this.enabled = enabled == null
                        ? Boolean.parseBoolean(effectiveProperties.getOrDefault(CONF_ENABLED, Boolean.TRUE.toString()))
                        : enabled;
                this.ignoreErrorAtSessionEnd = ignoreErrorAtSessionEnd == null
                        ? Boolean.parseBoolean(effectiveProperties.getOrDefault(
                                CONF_IGNORE_ERROR_AT_SESSION_END, Boolean.FALSE.toString()))
                        : ignoreErrorAtSessionEnd;

                this.localHostHint = effectiveProperties.get(CONF_LOCAL_HOST_HINT);
                this.localNodeInstance = localNodeInstance;
                this.repositorySystemSession = repositorySystemSession;
                this.resolverConnectorEnabled = resolverConnectorEnabled;
                this.resolverResolverPostProcessorEnabled = resolverResolverPostProcessorEnabled;
                this.resolverTrustedChecksumsSourceEnabled = resolverTrustedChecksumsSourceEnabled;

                // session impl (derived from those above)

                String keyMapper = SimpleKeyMapperFactory.NAME;
                Set<String> overlayNodes = Set.of();
                String localNode = localNodeInstance != null ? localNodeInstance.name() : "daemon";
                Set<String> repositories = RemoteRepositories.DEFAULT;
                Set<String> mirrors = Set.of();

                if (effectiveProperties.containsKey("mimir.session.keyMapper")) {
                    keyMapper = effectiveProperties.get("mimir.session.keyMapper");
                }
                if (effectiveProperties.containsKey("mimir.session.overlayNodes")) {
                    overlayNodes = Set.copyOf(Arrays.asList(effectiveProperties
                            .get("mimir.session.overlayNodes")
                            .split(",")));
                }
                if (localNodeInstance == null && effectiveProperties.containsKey("mimir.session.localNode")) {
                    localNode = effectiveProperties.get("mimir.session.localNode");
                }
                if (effectiveProperties.containsKey("mimir.session.repositories")) {
                    String value = effectiveProperties.get("mimir.session.repositories");
                    repositories = Set.copyOf(Arrays.asList(value.split(",")));
                }
                if (effectiveProperties.containsKey("mimir.session.mirrors")) {
                    String value = effectiveProperties.get("mimir.session.mirrors");
                    mirrors = Set.copyOf(Arrays.asList(value.split(",")));
                }

                this.keyMapper = keyMapper;
                this.overlayNodes = overlayNodes;
                this.localNode = localNode;
                this.repositories = repositories;
                this.mirrors = mirrors;
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
            public Path baseLocksDir() {
                return baseLocksDir;
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

            // session impl

            @Override
            public String keyMapper() {
                return keyMapper;
            }

            @Override
            public Set<String> overlayNodes() {
                return overlayNodes;
            }

            @Override
            public String localNode() {
                return localNode;
            }

            @Override
            public Optional<LocalNode> localNodeInstance() {
                return Optional.ofNullable(localNodeInstance);
            }

            @Override
            public Set<String> repositories() {
                return repositories;
            }

            @Override
            public Set<String> mirrors() {
                return mirrors;
            }

            @Override
            public boolean resolverConnectorEnabled() {
                return resolverConnectorEnabled;
            }

            @Override
            public boolean resolverTrustedChecksumsSourceEnabled() {
                return resolverTrustedChecksumsSourceEnabled;
            }

            @Override
            public boolean resolverResolverPostProcessorEnabled() {
                return resolverResolverPostProcessorEnabled;
            }
        }
    }
}
