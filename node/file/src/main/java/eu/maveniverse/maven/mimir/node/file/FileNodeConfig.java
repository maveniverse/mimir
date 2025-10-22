/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.node.file;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

import eu.maveniverse.maven.mimir.shared.SessionConfig;
import eu.maveniverse.maven.mimir.shared.impl.naming.SimpleKeyResolverFactory;
import eu.maveniverse.maven.shared.core.fs.FileUtils;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

public final class FileNodeConfig {
    /**
     * The cache purge mode.
     * Cache purge operates on <em>single artifact</em>, not over all constituents of a GAV (POM, JAR, ...). A GACEV is
     * either purged from cache or preserved.
     * Note: to use any purge mode other than {@link #OFF}, the exclusive access to store must be enabled
     * via config as well, see {@link FileNodeConfig#exclusiveAccess()}.
     */
    public enum CachePurge {
        /**
         * The default mode, no cache reduction happens. In this mode, cache just grows, collects all seen artifacts.
         * This is good mode for workstations, as it makes sure more and more builds can be served with cached artifacts.
         * On the other hand, on CI this may not be the thing you want, as cache sizes will grow endlessly (with new
         * artifacts being added).
         */
        OFF,
        /**
         * Cache reduction mode that performs reduction on begin, when node is started. In this mode the cache is
         * atomically moved to "shadow", and on every request entry pulled from "shadow" back into its place. When
         * node is closed, the "shadow" is deleted, with all remaining artifacts, and the file node store will contain
         * only the touched artifacts.
         * <p>
         * Positive side of this mode is that it does not require daemon shutdown to perform purge, but the cost is
         * that in case of failure, cache may be lost (cache archival in case of any failure on CI should not happen).
         * Once the build is done, it is guaranteed that cache contains all the touched artifacts, daemon needs no
         * special care. Also, this mode uses atomic moves, so overall storage consumption grows only with newly
         * cached items.
         */
        ON_BEGIN,
        /**
         * Cache reduction mode that performs reduction on end, when node is stopped. In this mode the cache is
         * left in place, and on every request entry is copied to "shadow" location. When node is closed,
         * the file node store and "shadow" are swapped, and the file node store will contain only the touched artifacts.
         * <p>
         * Positive side of this mode is that it is safer against data loss (cache is unchanged in case of crash or
         * failure), at the cost of explicit daemon shutdown requirement, since purge happens on file node close.
         * Storage wise this mode requires more space, as it uses copy operation and cache entries are duplicated at
         * one point (cleaned up at close).
         */
        ON_END
    }

    public static FileNodeConfig with(SessionConfig sessionConfig) {
        requireNonNull(sessionConfig, "config");

        Path basedir = sessionConfig.basedir().resolve("local");
        Path baseLockDir = sessionConfig.baseLocksDir().resolve(NAME);
        boolean mayLink = true;
        List<String> checksumAlgorithms = Arrays.asList("SHA-1", "SHA-512");
        String keyResolver = SimpleKeyResolverFactory.NAME;
        boolean exclusiveAccess = false;
        CachePurge cachePurge = CachePurge.OFF;

        if (sessionConfig.effectiveProperties().containsKey("mimir.file.basedir")) {
            basedir = FileUtils.canonicalPath(
                    Path.of(sessionConfig.effectiveProperties().get("mimir.file.basedir")));
        }
        if (sessionConfig.effectiveProperties().containsKey("mimir.file.mayLink")) {
            mayLink = Boolean.parseBoolean(sessionConfig.effectiveProperties().get("mimir.file.mayLink"));
        }
        if (sessionConfig.effectiveProperties().containsKey("mimir.file.checksumAlgorithms")) {
            checksumAlgorithms = Arrays.stream(sessionConfig
                            .effectiveProperties()
                            .get("mimir.file.checksumAlgorithms")
                            .split(","))
                    .filter(s -> !s.trim().isEmpty())
                    .collect(toList());
        }
        if (sessionConfig.effectiveProperties().containsKey("mimir.file.keyResolver")) {
            keyResolver = sessionConfig.effectiveProperties().get("mimir.file.keyResolver");
        }
        if (sessionConfig.effectiveProperties().containsKey("mimir.file.exclusiveAccess")) {
            exclusiveAccess =
                    Boolean.parseBoolean(sessionConfig.effectiveProperties().get("mimir.file.exclusiveAccess"));
        }
        if (sessionConfig.effectiveProperties().containsKey("mimir.file.cachePurge")) {
            cachePurge = CachePurge.valueOf(sessionConfig.effectiveProperties().get("mimir.file.cachePurge"));
        }

        return new FileNodeConfig(
                basedir, baseLockDir, mayLink, checksumAlgorithms, keyResolver, exclusiveAccess, cachePurge);
    }

    public static FileNodeConfig of(
            Path basedir,
            Path baseLockDir,
            boolean mayLink,
            List<String> checksumAlgorithms,
            String keyResolver,
            boolean exclusiveAccess,
            CachePurge cachePurge) {
        return new FileNodeConfig(
                basedir, baseLockDir, mayLink, checksumAlgorithms, keyResolver, exclusiveAccess, cachePurge);
    }

    public static final String NAME = "file";

    private final Path basedir;
    private final Path baseLockDir;
    private final boolean mayLink;
    private final List<String> checksumAlgorithms;
    private final String keyResolver;
    private final boolean exclusiveAccess;
    private final CachePurge cachePurge;

    private FileNodeConfig(
            Path basedir,
            Path baseLockDir,
            boolean mayLink,
            List<String> checksumAlgorithms,
            String keyResolver,
            boolean exclusiveAccess,
            CachePurge cachePurge) {
        this.basedir = basedir;
        this.baseLockDir = baseLockDir;
        this.mayLink = mayLink;
        this.checksumAlgorithms = List.copyOf(checksumAlgorithms);
        this.keyResolver = keyResolver;
        this.exclusiveAccess = exclusiveAccess;
        this.cachePurge = cachePurge;
        if (!exclusiveAccess && cachePurge != CachePurge.OFF) {
            throw new IllegalArgumentException(
                    "Invalid configuration: cachePurge possible only with exclusiveAccess enabled");
        }
    }

    public Path basedir() {
        return basedir;
    }

    public Path baseLockDir() {
        return baseLockDir;
    }

    public boolean mayLink() {
        return mayLink;
    }

    public List<String> checksumAlgorithms() {
        return checksumAlgorithms;
    }

    public String keyResolver() {
        return keyResolver;
    }

    public boolean exclusiveAccess() {
        return exclusiveAccess;
    }

    public CachePurge cachePurge() {
        return cachePurge;
    }
}
