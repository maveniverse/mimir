/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.extension3;

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
        if (supports(session, repository)) {
            return new MimirRepositoryConnector(
                    MimirUtils.requireSession(session),
                    repository,
                    basicRepositoryConnectorFactory.newInstance(session, repository));
        } else {
            return repositoryConnector;
        }
    }

    @Override
    public float getPriority() {
        return 10;
    }

    private boolean supports(RepositorySystemSession session, RemoteRepository repository) {
        // for now we do only "real remote" artifact caching, those coming over HTTP only (but this includes S3/minio)
        String protocol = repository.getProtocol();
        return protocol != null && protocol.contains("http");
    }
}
