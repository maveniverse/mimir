/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.node.minio;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

import eu.maveniverse.maven.mimir.shared.SessionConfig;
import eu.maveniverse.maven.mimir.shared.impl.naming.SimpleKeyResolverFactory;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class MinioNodeConfig {
    public static MinioNodeConfig with(SessionConfig sessionConfig) throws IOException {
        requireNonNull(sessionConfig, "config");

        String endpoint = "http://127.0.0.1:9000";
        String accessKey = "minioadmin";
        String secretKey = "minioadmin";
        List<String> checksumAlgorithms = Arrays.asList("SHA-1", "SHA-512");
        String keyResolver = SimpleKeyResolverFactory.NAME;
        boolean exclusiveAccess = false;
        boolean cachePurge = false;

        if (sessionConfig.effectiveProperties().containsKey("mimir.minio.endpoint")) {
            endpoint = sessionConfig.effectiveProperties().get("mimir.minio.endpoint");
        }
        if (sessionConfig.effectiveProperties().containsKey("mimir.minio.accessKey")) {
            accessKey = sessionConfig.effectiveProperties().get("mimir.minio.accessKey");
        }
        if (sessionConfig.effectiveProperties().containsKey("mimir.minio.secretKey")) {
            secretKey = sessionConfig.effectiveProperties().get("mimir.minio.secretKey");
        }
        if (sessionConfig.effectiveProperties().containsKey("mimir.minio.checksumAlgorithms")) {
            checksumAlgorithms = Arrays.stream(sessionConfig
                            .effectiveProperties()
                            .get("mimir.minio.checksumAlgorithms")
                            .split(","))
                    .filter(s -> !s.trim().isEmpty())
                    .collect(toList());
        }
        if (sessionConfig.effectiveProperties().containsKey("mimir.minio.keyResolver")) {
            keyResolver = sessionConfig.effectiveProperties().get("mimir.minio.keyResolver");
        }
        if (sessionConfig.effectiveProperties().containsKey("mimir.minio.exclusiveAccess")) {
            exclusiveAccess =
                    Boolean.parseBoolean(sessionConfig.effectiveProperties().get("mimir.minio.exclusiveAccess"));
        }
        if (sessionConfig.effectiveProperties().containsKey("mimir.minio.cachePurge")) {
            cachePurge =
                    Boolean.parseBoolean(sessionConfig.effectiveProperties().get("mimir.minio.cachePurge"));
        }
        return new MinioNodeConfig(
                endpoint, accessKey, secretKey, checksumAlgorithms, keyResolver, exclusiveAccess, cachePurge);
    }

    public static final String NAME = "minio";

    private final String endpoint;
    private final String accessKey;
    private final String secretKey;
    private final List<String> checksumAlgorithms;
    private final String keyResolver;
    private final boolean exclusiveAccess;
    private final boolean cachePurge;

    private MinioNodeConfig(
            String endpoint,
            String accessKey,
            String secretKey,
            List<String> checksumAlgorithms,
            String keyResolver,
            boolean exclusiveAccess,
            boolean cachePurge) {
        this.endpoint = endpoint;
        this.accessKey = accessKey;
        this.secretKey = secretKey;
        this.checksumAlgorithms = checksumAlgorithms;
        this.keyResolver = keyResolver;
        this.exclusiveAccess = exclusiveAccess;
        this.cachePurge = cachePurge;
    }

    public String endpoint() {
        return endpoint;
    }

    public String accessKey() {
        return accessKey;
    }

    public String secretKey() {
        return secretKey;
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
