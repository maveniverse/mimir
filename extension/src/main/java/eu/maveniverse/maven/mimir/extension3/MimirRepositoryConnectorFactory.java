/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.extension3;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.mimir.shared.MimirUtils;
import eu.maveniverse.maven.mimir.shared.Session;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import org.eclipse.aether.RepositorySystemSession;
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

    private final Map<String, Provider<RepositoryConnectorFactory>> repositoryConnectorFactories;
    private final ChecksumAlgorithmFactorySelector checksumAlgorithmFactorySelector;

    @Inject
    public MimirRepositoryConnectorFactory(
            Map<String, Provider<RepositoryConnectorFactory>> repositoryConnectorFactories,
            ChecksumAlgorithmFactorySelector checksumAlgorithmFactorySelector) {
        this.repositoryConnectorFactories =
                requireNonNull(repositoryConnectorFactories, "repositoryConnectorFactories");
        this.checksumAlgorithmFactorySelector =
                requireNonNull(checksumAlgorithmFactorySelector, "checksumAlgorithmFactorySelector");
    }

    @Override
    public RepositoryConnector newInstance(RepositorySystemSession session, RemoteRepository repository)
            throws NoRepositoryConnectorException {
        String message = "Mimir is disabled";
        Optional<Session> sessionOptional = MimirUtils.mayGetSession(session);
        if (sessionOptional.isPresent()) {
            Session mimirSession = sessionOptional.orElseThrow();
            message = "Unsupported repository: " + repository;
            if (mimirSession.repositorySupported(repository)) {
                RepositoryConnectorFactory basicRepositoryConnectorFactory = requireNonNull(
                        repositoryConnectorFactories.get("basic").get(), "No basic repository connector factory found");
                RepositoryConnector repositoryConnector =
                        basicRepositoryConnectorFactory.newInstance(session, repository);
                List<ChecksumAlgorithmFactory> checksumsAlgorithms = checksumAlgorithmFactorySelector.selectList(
                        ConfigUtils.parseCommaSeparatedUniqueNames(ConfigUtils.getString(
                                session,
                                "SHA-1,MD5", // copied from Maven2RepositoryLayoutFactory
                                "aether.checksums.algorithms." + repository.getId(),
                                "aether.checksums.algorithms",
                                "aether.layout.maven2.checksumAlgorithms" + repository.getId(),
                                "aether.layout.maven2.checksumAlgorithms")));
                return new MimirRepositoryConnector(
                        mimirSession,
                        repository,
                        repositoryConnector,
                        checksumsAlgorithms,
                        checksumAlgorithmFactorySelector.getChecksumAlgorithmFactories().stream()
                                .collect(Collectors.toMap(ChecksumAlgorithmFactory::getName, f -> f)));
            }
        }
        throw new NoRepositoryConnectorException(repository, message);
    }

    @Override
    public float getPriority() {
        return 10;
    }
}
