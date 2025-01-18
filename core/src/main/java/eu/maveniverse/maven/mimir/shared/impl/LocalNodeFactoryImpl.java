/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.shared.impl;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.mimir.shared.Config;
import eu.maveniverse.maven.mimir.shared.naming.KeyResolver;
import eu.maveniverse.maven.mimir.shared.naming.KeyResolverFactory;
import eu.maveniverse.maven.mimir.shared.node.LocalNode;
import eu.maveniverse.maven.mimir.shared.node.LocalNodeFactory;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import org.eclipse.aether.spi.connector.checksum.ChecksumAlgorithmFactory;

@Singleton
@Named
public final class LocalNodeFactoryImpl implements LocalNodeFactory {
    private final Map<String, KeyResolverFactory> keyResolverFactories;
    private final Map<String, ChecksumAlgorithmFactory> checksumFactories;

    @Inject
    public LocalNodeFactoryImpl(
            Map<String, KeyResolverFactory> keyResolverFactories,
            Map<String, ChecksumAlgorithmFactory> checksumFactories) {
        this.keyResolverFactories = requireNonNull(keyResolverFactories, "keyResolverFactories");
        this.checksumFactories = requireNonNull(checksumFactories, "checksumFactories");
    }

    @Override
    public LocalNode createLocalNode(Config config) throws IOException {
        requireNonNull(config, "config");
        LocalNodeConfig localNodeConfig = LocalNodeConfig.with(config);
        KeyResolverFactory keyResolverFactory = keyResolverFactories.get(localNodeConfig.keyResolver());
        if (keyResolverFactory == null) {
            throw new IllegalArgumentException("Unknown keyResolver: " + localNodeConfig.keyResolver());
        }
        KeyResolver keyResolver = requireNonNull(keyResolverFactory.createKeyResolver(config), "keyResolver");
        Map<String, ChecksumAlgorithmFactory> localChecksumFactories = new HashMap<>();
        for (String alg : localNodeConfig.checksumAlgorithms()) {
            ChecksumAlgorithmFactory checksumAlgorithmFactory = checksumFactories.get(alg);
            if (checksumAlgorithmFactory == null) {
                throw new IllegalArgumentException("Unknown checksumAlgorithmFactory: " + alg);
            }
            localChecksumFactories.put(alg, checksumAlgorithmFactory);
        }
        return new LocalNodeImpl(
                localNodeConfig.name(),
                localNodeConfig.distance(),
                localNodeConfig.basedir(),
                keyResolver,
                localChecksumFactories);
    }
}
