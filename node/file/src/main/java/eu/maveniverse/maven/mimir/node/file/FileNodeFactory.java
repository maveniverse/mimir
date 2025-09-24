/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.node.file;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.mimir.shared.SessionConfig;
import eu.maveniverse.maven.mimir.shared.naming.KeyResolver;
import eu.maveniverse.maven.mimir.shared.naming.KeyResolverFactory;
import eu.maveniverse.maven.mimir.shared.node.LocalNodeFactory;
import eu.maveniverse.maven.mimir.shared.node.SystemNodeFactory;
import eu.maveniverse.maven.shared.core.fs.DirectoryLocker;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import org.eclipse.aether.spi.connector.checksum.ChecksumAlgorithmFactory;

@Singleton
@Named(FileNodeConfig.NAME)
public final class FileNodeFactory implements LocalNodeFactory<FileNode>, SystemNodeFactory<FileNode> {
    private final Map<String, KeyResolverFactory> keyResolverFactories;
    private final Map<String, ChecksumAlgorithmFactory> checksumFactories;

    @Inject
    public FileNodeFactory(
            Map<String, KeyResolverFactory> keyResolverFactories,
            Map<String, ChecksumAlgorithmFactory> checksumFactories) {
        this.keyResolverFactories = requireNonNull(keyResolverFactories, "keyResolverFactories");
        this.checksumFactories = requireNonNull(checksumFactories, "checksumFactories");
    }

    @Override
    public Optional<FileNode> createLocalNode(SessionConfig sessionConfig) throws IOException {
        return Optional.of(createSystemNode(sessionConfig));
    }

    @Override
    public FileNode createSystemNode(SessionConfig sessionConfig) throws IOException {
        requireNonNull(sessionConfig, "config");
        FileNodeConfig fileNodeConfig = FileNodeConfig.with(sessionConfig);
        KeyResolverFactory keyResolverFactory = keyResolverFactories.get(fileNodeConfig.keyResolver());
        if (keyResolverFactory == null) {
            throw new IllegalArgumentException("Unknown keyResolver: " + fileNodeConfig.keyResolver());
        }
        KeyResolver keyResolver = requireNonNull(keyResolverFactory.createKeyResolver(sessionConfig), "keyResolver");

        // verify checksum config
        for (String alg : fileNodeConfig.checksumAlgorithms()) {
            ChecksumAlgorithmFactory checksumAlgorithmFactory = checksumFactories.get(alg);
            if (checksumAlgorithmFactory == null) {
                throw new IllegalArgumentException("Unknown checksumAlgorithmFactory: " + alg);
            }
        }

        return new FileNode(
                fileNodeConfig.basedir(),
                fileNodeConfig.mayLink(),
                fileNodeConfig.exclusiveAccess(),
                fileNodeConfig.cachePurge(),
                keyResolver,
                fileNodeConfig.checksumAlgorithms(),
                checksumFactories,
                DirectoryLocker.INSTANCE);
    }
}
