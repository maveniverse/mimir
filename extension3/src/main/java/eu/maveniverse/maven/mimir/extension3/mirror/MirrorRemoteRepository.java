/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.extension3.mirror;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.shared.core.component.CloseableSupport;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.spi.connector.RepositoryConnector;

/**
 * Mirrored remote repository with all the connectors.
 */
public class MirrorRemoteRepository extends CloseableSupport {
    private final RemoteRepository repository;
    private final RepositoryConnector repositoryConnector;
    private final LinkedHashMap<RemoteRepository, RepositoryConnector> mirrorConnectors;

    public MirrorRemoteRepository(
            RemoteRepository repository,
            RepositoryConnector repositoryConnector,
            LinkedHashMap<RemoteRepository, RepositoryConnector> mirrorConnectors) {
        this.repository = requireNonNull(repository);
        this.repositoryConnector = requireNonNull(repositoryConnector);
        this.mirrorConnectors = requireNonNull(mirrorConnectors);
    }

    @Override
    protected void doClose() throws IOException {
        ArrayList<Exception> exceptions = new ArrayList<>();
        for (Map.Entry<RemoteRepository, RepositoryConnector> entry : mirrorConnectors.entrySet()) {
            try {
                entry.getValue().close();
            } catch (Exception e) {
                exceptions.add(e);
            }
        }
        try {
            repositoryConnector.close();
        } catch (Exception e) {
            exceptions.add(e);
        }
        if (!exceptions.isEmpty()) {
            IOException closeFailed = new IOException("Failed to close mirrored repository " + repository);
            exceptions.forEach(closeFailed::addSuppressed);
            throw closeFailed;
        }
    }

    public RemoteRepository getRepository() {
        return repository;
    }

    public RepositoryConnector getRepositoryConnector() {
        return repositoryConnector;
    }

    public LinkedHashMap<RemoteRepository, RepositoryConnector> getMirrorConnectors() {
        return mirrorConnectors;
    }
}
