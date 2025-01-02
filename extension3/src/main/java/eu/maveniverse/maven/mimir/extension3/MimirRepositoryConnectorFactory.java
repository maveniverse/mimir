/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.extension3;

import eu.maveniverse.maven.mimir.shared.Session;
import javax.inject.Inject;
import javax.inject.Named;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.spi.connector.RepositoryConnector;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.transfer.NoRepositoryConnectorException;

/**
 * Factory for now "hard wraps" basic, but it should be made smarter.
 */
@Named(MimirRepositoryConnectorFactory.NAME)
public class MimirRepositoryConnectorFactory implements RepositoryConnectorFactory {
    public static final String NAME = "mimir";

    private final BasicRepositoryConnectorFactory basicRepositoryConnectorFactory;

    @Inject
    public MimirRepositoryConnectorFactory(BasicRepositoryConnectorFactory basicRepositoryConnectorFactory) {
        this.basicRepositoryConnectorFactory = basicRepositoryConnectorFactory;
    }

    @Override
    public RepositoryConnector newInstance(RepositorySystemSession session, RemoteRepository repository)
            throws NoRepositoryConnectorException {
        RepositoryConnector repositoryConnector = basicRepositoryConnectorFactory.newInstance(session, repository);
        Session mimirSession = MimirUtils.requireSession(session);
        if (mimirSession.supports(repository)) {
            return new MimirRepositoryConnector(
                    mimirSession, repository, basicRepositoryConnectorFactory.newInstance(session, repository));
        } else {
            return repositoryConnector;
        }
    }

    @Override
    public float getPriority() {
        return 10;
    }
}
