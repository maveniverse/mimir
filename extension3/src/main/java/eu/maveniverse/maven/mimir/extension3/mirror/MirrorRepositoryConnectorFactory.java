/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.extension3.mirror;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.mimir.shared.MimirUtils;
import eu.maveniverse.maven.mimir.shared.Session;
import eu.maveniverse.maven.mimir.shared.mirror.MirroredRemoteRepository;
import eu.maveniverse.maven.shared.core.component.ComponentSupport;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.impl.RemoteRepositoryManager;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.spi.connector.RepositoryConnector;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.transfer.NoRepositoryConnectorException;

/**
 * Factory for mirrors.
 */
@Named(MirrorRepositoryConnectorFactory.NAME)
public class MirrorRepositoryConnectorFactory extends ComponentSupport implements RepositoryConnectorFactory {
    public static final String NAME = "mirror";

    private final RemoteRepositoryManager remoteRepositoryManager;
    private final RepositoryConnectorFactory basicRepositoryConnectorFactory;

    @Inject
    public MirrorRepositoryConnectorFactory(
            RemoteRepositoryManager remoteRepositoryManager,
            Map<String, Provider<RepositoryConnectorFactory>> repositoryConnectorFactories) {
        this.remoteRepositoryManager = requireNonNull(remoteRepositoryManager);
        this.basicRepositoryConnectorFactory = requireNonNull(
                repositoryConnectorFactories.get("basic").get(), "No basic repository connector factory found");
    }

    @Override
    public RepositoryConnector newInstance(RepositorySystemSession session, RemoteRepository repository)
            throws NoRepositoryConnectorException {
        Session ms = MimirUtils.mayGetSession(session).orElse(null);
        if (ms != null && ms.config().resolverConnectorEnabled()) {
            Optional<MirroredRemoteRepository> mrro = ms.repositoryMirror(repository);
            if (mrro.isPresent()) {
                MirroredRemoteRepository mrr = mrro.orElseThrow();

                RepositoryConnector repositoryConnector =
                        basicRepositoryConnectorFactory.newInstance(session, mrr.remoteRepository());
                LinkedHashMap<RemoteRepository, RepositoryConnector> mirrorConnectors = new LinkedHashMap<>();
                for (RemoteRepository mirror : mrr.remoteRepositoryMirrors()) {
                    mirrorConnectors.put(
                            mirror,
                            basicRepositoryConnectorFactory.newInstance(
                                    session,
                                    new RemoteRepository.Builder(remoteRepositoryManager
                                                    .aggregateRepositories(
                                                            session, new ArrayList<>(), List.of(mirror), true)
                                                    .get(0))
                                            .setId(repository.getId())
                                            .setMirroredRepositories(List.of(repository))
                                            .build()));
                }
                return new MirrorRepositoryConnector(
                        new MirrorRemoteRepository(repository, repositoryConnector, mirrorConnectors));
            }
        }
        return basicRepositoryConnectorFactory.newInstance(session, repository);
    }

    @Override
    public float getPriority() {
        return 5;
    }
}
