/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.node.minio;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.mimir.shared.Config;
import eu.maveniverse.maven.mimir.shared.checksum.ChecksumAlgorithmFactory;
import eu.maveniverse.maven.mimir.shared.naming.KeyResolver;
import eu.maveniverse.maven.mimir.shared.naming.KeyResolverFactory;
import eu.maveniverse.maven.mimir.shared.node.SystemNodeFactory;
import io.minio.MinioClient;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

@Singleton
@Named(MinioNodeConfig.NAME)
public class MinioNodeFactory implements SystemNodeFactory {
    private final Map<String, KeyResolverFactory> keyResolverFactories;
    private final Map<String, ChecksumAlgorithmFactory> checksumFactories;

    @Inject
    public MinioNodeFactory(
            Map<String, KeyResolverFactory> keyResolverFactories,
            Map<String, ChecksumAlgorithmFactory> checksumFactories) {
        this.keyResolverFactories = requireNonNull(keyResolverFactories, "keyResolverFactories");
        this.checksumFactories = requireNonNull(checksumFactories, "checksumFactories");
    }

    @Override
    public MinioNode createNode(Config config) throws IOException {
        MinioNodeConfig minioNodeConfig = MinioNodeConfig.with(config);
        KeyResolverFactory keyResolverFactory = keyResolverFactories.get(minioNodeConfig.keyResolver());
        if (keyResolverFactory == null) {
            throw new IllegalArgumentException("Unknown keyResolver: " + minioNodeConfig.keyResolver());
        }
        KeyResolver keyResolver = requireNonNull(keyResolverFactory.createKeyResolver(config), "keyResolver");
        HashMap<String, ChecksumAlgorithmFactory> localChecksumFactories = new HashMap<>();
        for (String alg : minioNodeConfig.checksumAlgorithms()) {
            ChecksumAlgorithmFactory checksumAlgorithmFactory = checksumFactories.get(alg);
            if (checksumAlgorithmFactory == null) {
                throw new IllegalArgumentException("Unknown checksumAlgorithmFactory: " + alg);
            }
            localChecksumFactories.put(alg, checksumAlgorithmFactory);
        }
        MinioClient minioClient = createMinioClient(minioNodeConfig);
        return new MinioNode(
                minioNodeConfig,
                minioClient,
                keyResolver,
                minioNodeConfig.checksumAlgorithms(),
                localChecksumFactories);
    }

    private MinioClient createMinioClient(MinioNodeConfig minioNodeConfig) {
        return MinioClient.builder()
                .endpoint(minioNodeConfig.endpoint())
                .credentials(minioNodeConfig.accessKey(), minioNodeConfig.secretKey())
                .build();
    }
}
