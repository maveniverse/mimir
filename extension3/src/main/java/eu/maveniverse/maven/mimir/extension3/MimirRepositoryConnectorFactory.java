/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.extension3;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.mimir.extension3.mirror.MirrorRepositoryConnectorFactory;
import eu.maveniverse.maven.mimir.shared.MimirUtils;
import eu.maveniverse.maven.mimir.shared.Session;
import eu.maveniverse.maven.shared.core.component.ComponentSupport;
import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Named;
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
public class MimirRepositoryConnectorFactory extends ComponentSupport implements RepositoryConnectorFactory {
    public static final String NAME = "mimir";

    private final MirrorRepositoryConnectorFactory mirrorRepositoryConnectorFactory;
    private final ChecksumAlgorithmFactorySelector checksumAlgorithmFactorySelector;

    @Inject
    public MimirRepositoryConnectorFactory(
            MirrorRepositoryConnectorFactory mirrorRepositoryConnectorFactory,
            ChecksumAlgorithmFactorySelector checksumAlgorithmFactorySelector) {
        this.mirrorRepositoryConnectorFactory =
                requireNonNull(mirrorRepositoryConnectorFactory, "mirrorRepositoryConnectorFactory");
        this.checksumAlgorithmFactorySelector =
                requireNonNull(checksumAlgorithmFactorySelector, "checksumAlgorithmFactorySelector");
    }

    @Override
    public RepositoryConnector newInstance(RepositorySystemSession session, RemoteRepository repository)
            throws NoRepositoryConnectorException {
        String message = "Mimir is disabled";
        Session ms = MimirUtils.mayGetSession(session).orElse(null);
        if (ms != null && ms.config().resolverConnectorEnabled()) {
            message = "Unsupported repository: " + repository;
            if (ms.repositorySupported(repository)) {
                RepositoryConnector repositoryConnector =
                        mirrorRepositoryConnectorFactory.newInstance(session, repository);
                List<ChecksumAlgorithmFactory> checksumsAlgorithms = checksumAlgorithmFactorySelector.selectList(
                        ConfigUtils.parseCommaSeparatedUniqueNames(ConfigUtils.getString(
                                session,
                                "SHA-1,MD5", // copied from Maven2RepositoryLayoutFactory
                                "aether.checksums.algorithms." + repository.getId(),
                                "aether.checksums.algorithms",
                                "aether.layout.maven2.checksumAlgorithms" + repository.getId(),
                                "aether.layout.maven2.checksumAlgorithms")));
                return new MimirRepositoryConnector(
                        ms,
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
