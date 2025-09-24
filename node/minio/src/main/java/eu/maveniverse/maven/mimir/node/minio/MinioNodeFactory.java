/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.node.minio;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.mimir.shared.SessionConfig;
import eu.maveniverse.maven.mimir.shared.naming.KeyResolver;
import eu.maveniverse.maven.mimir.shared.naming.KeyResolverFactory;
import eu.maveniverse.maven.mimir.shared.node.SystemNodeFactory;
import io.minio.MinioClient;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import org.eclipse.aether.spi.connector.checksum.ChecksumAlgorithmFactory;

@Singleton
@Named(MinioNodeConfig.NAME)
public class MinioNodeFactory implements SystemNodeFactory<MinioNode> {
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
    public Optional<MinioNode> createNode(SessionConfig sessionConfig) throws IOException {
        MinioNodeConfig minioNodeConfig = MinioNodeConfig.with(sessionConfig);
        KeyResolverFactory keyResolverFactory = keyResolverFactories.get(minioNodeConfig.keyResolver());
        if (keyResolverFactory == null) {
            throw new IllegalArgumentException("Unknown keyResolver: " + minioNodeConfig.keyResolver());
        }
        KeyResolver keyResolver = requireNonNull(keyResolverFactory.createKeyResolver(sessionConfig), "keyResolver");

        // verify checksums
        for (String alg : minioNodeConfig.checksumAlgorithms()) {
            ChecksumAlgorithmFactory checksumAlgorithmFactory = checksumFactories.get(alg);
            if (checksumAlgorithmFactory == null) {
                throw new IllegalArgumentException("Unknown checksumAlgorithmFactory: " + alg);
            }
        }
        MinioClient minioClient = createMinioClient(minioNodeConfig);
        return Optional.of(new MinioNode(
                minioNodeConfig,
                minioClient,
                minioNodeConfig.exclusiveAccess(),
                minioNodeConfig.cachePurge(),
                keyResolver,
                minioNodeConfig.checksumAlgorithms(),
                checksumFactories));
    }

    private MinioClient createMinioClient(MinioNodeConfig minioNodeConfig) {
        return MinioClient.builder()
                .endpoint(minioNodeConfig.endpoint())
                .credentials(minioNodeConfig.accessKey(), minioNodeConfig.secretKey())
                .build();
    }
}
