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

import eu.maveniverse.maven.mimir.shared.Config;
import eu.maveniverse.maven.mimir.shared.impl.SimpleKeyResolverFactory;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class MinioNodeConfig {
    public static MinioNodeConfig with(Config config) throws IOException {
        requireNonNull(config, "config");

        boolean publisherEnabled = true;
        String publisherTransport = "socket";
        String endpoint = "http://127.0.0.1:9000";
        String accessKey = "minioadmin";
        String secretKey = "minioadmin";
        List<String> checksumAlgorithms = Arrays.asList("SHA-1", "SHA-512");
        String keyResolver = SimpleKeyResolverFactory.NAME;

        if (config.effectiveProperties().containsKey("mimir.minio.publisher.enabled")) {
            publisherEnabled = Boolean.parseBoolean(config.effectiveProperties().get("mimir.minio.publisher.enabled"));
        }
        if (config.effectiveProperties().containsKey("mimir.minio.publisher.transport")) {
            publisherTransport = config.effectiveProperties().get("mimir.minio.publisher.transport");
        }
        if (config.effectiveProperties().containsKey("mimir.minio.endpoint")) {
            endpoint = config.effectiveProperties().get("mimir.minio.endpoint");
        }
        if (config.effectiveProperties().containsKey("mimir.minio.accessKey")) {
            accessKey = config.effectiveProperties().get("mimir.minio.accessKey");
        }
        if (config.effectiveProperties().containsKey("mimir.minio.secretKey")) {
            secretKey = config.effectiveProperties().get("mimir.minio.secretKey");
        }
        if (config.effectiveProperties().containsKey("mimir.minio.checksumAlgorithms")) {
            checksumAlgorithms = Arrays.stream(config.effectiveProperties()
                            .get("mimir.minio.checksumAlgorithms")
                            .split(","))
                    .filter(s -> !s.trim().isEmpty())
                    .collect(toList());
        }
        if (config.effectiveProperties().containsKey("mimir.minio.keyResolver")) {
            keyResolver = config.effectiveProperties().get("mimir.minio.keyResolver");
        }
        return new MinioNodeConfig(
                publisherEnabled, publisherTransport, endpoint, accessKey, secretKey, checksumAlgorithms, keyResolver);
    }

    public static final String NAME = "minio";

    private final boolean publisherEnabled;
    private final String publisherTransport;
    private final String endpoint;
    private final String accessKey;
    private final String secretKey;
    private final List<String> checksumAlgorithms;
    private final String keyResolver;

    private MinioNodeConfig(
            boolean publisherEnabled,
            String publisherTransport,
            String endpoint,
            String accessKey,
            String secretKey,
            List<String> checksumAlgorithms,
            String keyResolver) {
        this.publisherEnabled = publisherEnabled;
        this.publisherTransport = publisherTransport;
        this.endpoint = endpoint;
        this.accessKey = accessKey;
        this.secretKey = secretKey;
        this.checksumAlgorithms = checksumAlgorithms;
        this.keyResolver = keyResolver;
    }

    public boolean publisherEnabled() {
        return publisherEnabled;
    }

    public String publisherTransport() {
        return publisherTransport;
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
}
