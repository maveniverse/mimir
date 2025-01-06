/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.extension3;

import eu.maveniverse.maven.mimir.shared.CacheEntry;
import eu.maveniverse.maven.mimir.shared.CacheKey;
import eu.maveniverse.maven.mimir.shared.Session;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.spi.connector.ArtifactDownload;
import org.eclipse.aether.spi.connector.ArtifactUpload;
import org.eclipse.aether.spi.connector.MetadataDownload;
import org.eclipse.aether.spi.connector.MetadataUpload;
import org.eclipse.aether.spi.connector.RepositoryConnector;
import org.eclipse.aether.transfer.ArtifactTransferException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mimir connector wraps another connector that does real job.
 */
public class MimirRepositoryConnector implements RepositoryConnector {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final Session mimirSession;
    private final RemoteRepository remoteRepository;
    private final RepositoryConnector delegate;

    public MimirRepositoryConnector(
            Session mimirSession, RemoteRepository remoteRepository, RepositoryConnector delegate) {
        this.mimirSession = mimirSession;
        this.remoteRepository = remoteRepository;
        this.delegate = delegate;
    }

    @Override
    public void get(
            Collection<? extends ArtifactDownload> artifactDownloads,
            Collection<? extends MetadataDownload> metadataDownloads) {
        // 1st round: provide whatever we have cached
        List<ArtifactDownload> ads = new ArrayList<>();
        HashMap<Artifact, CacheKey> keys = new HashMap<>();
        if (artifactDownloads != null && !artifactDownloads.isEmpty()) {
            for (ArtifactDownload artifactDownload : artifactDownloads) {
                if (artifactDownload.isExistenceCheck()) {
                    ads.add(artifactDownload);
                } else {
                    Optional<CacheKey> cacheKey =
                            mimirSession.cacheKey(remoteRepository, artifactDownload.getArtifact());
                    if (cacheKey.isPresent()) {
                        try {
                            CacheKey key = cacheKey.orElseThrow(() -> new IllegalStateException("Cache key not found"));
                            Optional<CacheEntry> entry = mimirSession.locate(key);
                            if (entry.isPresent()) {
                                try (CacheEntry ce =
                                        entry.orElseThrow(() -> new IllegalStateException("Cache entry not found"))) {
                                    logger.debug(
                                            "Fetched {} from Mimir '{}' cache",
                                            artifactDownload.getArtifact(),
                                            ce.origin());
                                    ce.transferTo(artifactDownload.getFile().toPath());
                                }
                            } else {
                                ads.add(artifactDownload);
                                keys.put(artifactDownload.getArtifact(), key);
                            }
                        } catch (IOException e) {
                            artifactDownload.setException(
                                    new ArtifactTransferException(artifactDownload.getArtifact(), remoteRepository, e));
                        }
                    } else {
                        ads.add(artifactDownload);
                    }
                }
            }
        }

        // unmatched ones are to be fetched by delegate
        delegate.get(ads, metadataDownloads);

        // 2nd round: those fetched (and healthy) should be cached
        if (!ads.isEmpty()) {
            for (ArtifactDownload artifactDownload : ads) {
                CacheKey cacheKey = keys.get(artifactDownload.getArtifact());
                if (cacheKey != null && artifactDownload.getException() == null) {
                    try {
                        logger.debug("Storing {} to Mimir 'local' cache", artifactDownload.getArtifact());
                        mimirSession.store(cacheKey, artifactDownload.getFile().toPath());
                    } catch (IOException e) {
                        artifactDownload.setException(
                                new ArtifactTransferException(artifactDownload.getArtifact(), remoteRepository, e));
                    }
                }
            }
        }
    }

    @Override
    public void put(
            Collection<? extends ArtifactUpload> artifactUploads,
            Collection<? extends MetadataUpload> metadataUploads) {
        // we do not interfere with PUTs
        delegate.put(artifactUploads, metadataUploads);
    }

    @Override
    public void close() {
        delegate.close();
    }
}
