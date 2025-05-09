/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.extension3;

import static java.util.Objects.requireNonNull;
import static org.eclipse.aether.internal.impl.Maven2RepositoryLayoutFactory.CONFIG_PROP_CHECKSUMS_ALGORITHMS;

import eu.maveniverse.maven.mimir.shared.Config;
import eu.maveniverse.maven.mimir.shared.Session;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Named;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.spi.connector.RepositoryConnector;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.checksum.ChecksumAlgorithmFactory;
import org.eclipse.aether.spi.connector.checksum.ChecksumAlgorithmFactorySelector;
import org.eclipse.aether.transfer.NoRepositoryConnectorException;
import org.eclipse.aether.util.ConfigUtils;

/**
 * Factory for now "hard wraps" basic, but it should be made smarter.
 */
@Named(MimirRepositoryConnectorFactory.NAME)
public class MimirRepositoryConnectorFactory implements RepositoryConnectorFactory {
    public static final String NAME = "mimir";

    private final BasicRepositoryConnectorFactory basicRepositoryConnectorFactory;
    private final ChecksumAlgorithmFactorySelector checksumAlgorithmFactorySelector;

    @Inject
    public MimirRepositoryConnectorFactory(
            BasicRepositoryConnectorFactory basicRepositoryConnectorFactory,
            ChecksumAlgorithmFactorySelector checksumAlgorithmFactorySelector) {
        this.basicRepositoryConnectorFactory =
                requireNonNull(basicRepositoryConnectorFactory, "basicRepositoryConnectorFactory");
        this.checksumAlgorithmFactorySelector =
                requireNonNull(checksumAlgorithmFactorySelector, "checksumAlgorithmFactorySelector");
    }

    @Override
    public RepositoryConnector newInstance(RepositorySystemSession session, RemoteRepository repository)
            throws NoRepositoryConnectorException {
        String message = "Mimir is disabled";
        Optional<Config> mimirConfigOptional = MimirUtils.mayGetConfig(session);
        if (mimirConfigOptional.isPresent()
                && mimirConfigOptional
                        .orElseThrow(() -> new IllegalStateException("Value is not present"))
                        .enabled()) {
            message = "Mimir session not yet created";
            Optional<Session> mimirSessionOptional = MimirUtils.mayGetSession(session);
            if (mimirSessionOptional.isPresent()) {
                Session mimirSession =
                        mimirSessionOptional.orElseThrow(() -> new IllegalStateException("Value is not present"));
                message = "Unsupported repository: " + repository;
                if (mimirSession.repositorySupported(repository)) {
                    RepositoryConnector repositoryConnector =
                            basicRepositoryConnectorFactory.newInstance(session, repository);
                    List<ChecksumAlgorithmFactory> checksumsAlgorithms = checksumAlgorithmFactorySelector.selectList(
                            ConfigUtils.parseCommaSeparatedUniqueNames(ConfigUtils.getString(
                                    session,
                                    "SHA-1,MD5", // copied from Maven2RepositoryLayoutFactory
                                    CONFIG_PROP_CHECKSUMS_ALGORITHMS + "." + repository.getId(),
                                    CONFIG_PROP_CHECKSUMS_ALGORITHMS)));
                    return new MimirRepositoryConnector(
                            mimirSession,
                            repository,
                            repositoryConnector,
                            checksumsAlgorithms,
                            checksumAlgorithmFactorySelector.getChecksumAlgorithmFactories().stream()
                                    .collect(Collectors.toMap(ChecksumAlgorithmFactory::getName, f -> f)));
                }
            }
        }
        throw new NoRepositoryConnectorException(repository, message);
    }

    @Override
    public float getPriority() {
        return 10;
    }
}
