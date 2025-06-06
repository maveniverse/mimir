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
    public static FileNodeConfig with(SessionConfig sessionConfig) {
        requireNonNull(sessionConfig, "config");

        Path basedir = sessionConfig.basedir().resolve("local");
        boolean mayLink = true;
        List<String> checksumAlgorithms = Arrays.asList("SHA-1", "SHA-512");
        String keyResolver = SimpleKeyResolverFactory.NAME;
        boolean exclusiveAccess = false;
        boolean cachePurge = false;

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
            cachePurge =
                    Boolean.parseBoolean(sessionConfig.effectiveProperties().get("mimir.file.cachePurge"));
        }

        return new FileNodeConfig(basedir, mayLink, checksumAlgorithms, keyResolver, exclusiveAccess, cachePurge);
    }

    public static FileNodeConfig of(
            Path basedir,
            boolean mayLink,
            List<String> checksumAlgorithms,
            String keyResolver,
            boolean exclusiveAccess,
            boolean cachePurge) {
        return new FileNodeConfig(
                FileUtils.canonicalPath(basedir),
                mayLink,
                checksumAlgorithms,
                keyResolver,
                exclusiveAccess,
                cachePurge);
    }

    public static final String NAME = "file";

    private final Path basedir;
    private final boolean mayLink;
    private final List<String> checksumAlgorithms;
    private final String keyResolver;
    private final boolean exclusiveAccess;
    private final boolean cachePurge;

    private FileNodeConfig(
            Path basedir,
            boolean mayLink,
            List<String> checksumAlgorithms,
            String keyResolver,
            boolean exclusiveAccess,
            boolean cachePurge) {
        this.basedir = basedir;
        this.mayLink = mayLink;
        this.checksumAlgorithms = List.copyOf(checksumAlgorithms);
        this.keyResolver = keyResolver;
        this.exclusiveAccess = exclusiveAccess;
        this.cachePurge = cachePurge;
    }

    public Path basedir() {
        return basedir;
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

    public boolean cachePurge() {
        return cachePurge;
    }
}
