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
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.spi.connector.ArtifactDownload;
import org.eclipse.aether.spi.connector.ArtifactUpload;
import org.eclipse.aether.spi.connector.MetadataDownload;
import org.eclipse.aether.spi.connector.MetadataUpload;
import org.eclipse.aether.spi.connector.RepositoryConnector;
import org.eclipse.aether.transfer.AbstractTransferListener;
import org.eclipse.aether.transfer.TransferEvent;
import org.eclipse.aether.util.listener.ChainedTransferListener;

/**
 * Mirror connector that handles multiple URLs for single {@link RemoteRepository}.
 */
public class MirrorRepositoryConnector extends CloseableSupport implements RepositoryConnector {
    private final MirrorRemoteRepository mirrorRemoteRepository;

    public MirrorRepositoryConnector(MirrorRemoteRepository mirrorRemoteRepository) {
        this.mirrorRemoteRepository = requireNonNull(mirrorRemoteRepository);
    }

    @Override
    public void get(
            Collection<? extends ArtifactDownload> artifactDownloads,
            Collection<? extends MetadataDownload> metadataDownloads) {
        List<? extends ArtifactDownload> ad = safe(artifactDownloads);
        final List<? extends ArtifactDownload> adRemainder = new CopyOnWriteArrayList<>(ad);
        ad.forEach(a -> {
            a.setListener(ChainedTransferListener.newInstance(a.getListener(), new AbstractTransferListener() {
                @Override
                public void transferSucceeded(TransferEvent event) {
                    adRemainder.remove(a);
                }
            }));
        });
        List<? extends MetadataDownload> md = safe(metadataDownloads);
        final List<? extends MetadataDownload> mdRemainder = new CopyOnWriteArrayList<>(md);
        md.forEach(m -> {
            m.setListener(ChainedTransferListener.newInstance(m.getListener(), new AbstractTransferListener() {
                @Override
                public void transferSucceeded(TransferEvent event) {
                    mdRemainder.remove(m);
                }
            }));
        });
        for (RepositoryConnector connector :
                mirrorRemoteRepository.getMirrorConnectors().values()) {
            if (!ad.isEmpty() || !md.isEmpty()) {
                connector.get(ad, md);
            } else {
                break;
            }
            if (!adRemainder.isEmpty() || !mdRemainder.isEmpty()) {
                ad = new ArrayList<>(adRemainder);
                md = new ArrayList<>(mdRemainder);
            }
        }
        if (!adRemainder.isEmpty() || !mdRemainder.isEmpty()) {
            mirrorRemoteRepository.getRepositoryConnector().get(ad, md);
        }
    }

    @Override
    public void put(
            Collection<? extends ArtifactUpload> artifactUploads,
            Collection<? extends MetadataUpload> metadataUploads) {
        mirrorRemoteRepository.getRepositoryConnector().put(artifactUploads, metadataUploads);
    }

    @Override
    public void close() {
        try {
            super.close();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    protected void doClose() throws IOException {
        mirrorRemoteRepository.close();
    }

    private static <T> List<T> safe(Collection<T> items) {
        return (items != null) ? new ArrayList<>(items) : Collections.emptyList();
    }
}
