/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.shared.impl;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.mimir.shared.Entry;
import eu.maveniverse.maven.mimir.shared.Session;
import eu.maveniverse.maven.mimir.shared.node.LocalEntry;
import eu.maveniverse.maven.mimir.shared.node.LocalNode;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.util.artifact.ArtifactIdUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SessionImpl implements Session {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final AtomicBoolean closed;
    private final Predicate<RemoteRepository> repositoryPredicate;
    private final Predicate<Artifact> artifactPredicate;
    private final BiFunction<RemoteRepository, Artifact, URI> keyMapper;
    private final LocalNode<?> localNode;
    private final Stats stats;

    private final ConcurrentHashMap<RemoteRepository, Set<String>> retrievedFromCache;
    private final ConcurrentHashMap<RemoteRepository, Set<String>> storedToCache;

    public SessionImpl(
            Predicate<RemoteRepository> repositoryPredicate,
            Predicate<Artifact> artifactPredicate,
            BiFunction<RemoteRepository, Artifact, URI> keyMapper,
            LocalNode<?> localNode) {
        this.closed = new AtomicBoolean(false);
        this.repositoryPredicate = requireNonNull(repositoryPredicate, "repositoryPredicate");
        this.artifactPredicate = requireNonNull(artifactPredicate, "artifactPredicate");
        this.keyMapper = requireNonNull(keyMapper, "nameMapper");
        this.localNode = requireNonNull(localNode, "localNode");
        this.stats = new Stats();

        this.retrievedFromCache = new ConcurrentHashMap<>();
        this.storedToCache = new ConcurrentHashMap<>();

        logger.info("Mimir session created with {}", localNode);
    }

    @Override
    public boolean repositorySupported(RemoteRepository repository) {
        checkState();
        return repositoryPredicate.test(repository);
    }

    @Override
    public boolean artifactSupported(Artifact artifact) {
        checkState();
        return artifactPredicate.test(artifact);
    }

    @Override
    public List<String> checksumAlgorithms() throws IOException {
        checkState();
        return localNode.checksumAlgorithms();
    }

    @Override
    public Optional<Entry> locate(RemoteRepository remoteRepository, Artifact artifact) throws IOException {
        checkState();
        requireNonNull(remoteRepository, "remoteRepository");
        requireNonNull(artifact, "artifact");
        if (repositoryPredicate.test(remoteRepository) && artifactPredicate.test(artifact)) {
            URI key = keyMapper.apply(remoteRepository, artifact);
            Optional<? extends LocalEntry> result = localNode.locate(key);
            if (result.isPresent()) {
                LocalEntry entry = result.orElseThrow();
                return stats.doLocate(Optional.of(new Entry() {
                    @Override
                    public void transferTo(Path file) throws IOException {
                        try {
                            entry.transferTo(file);
                            stats.doTransfer(true);
                            storedToCache
                                    .computeIfAbsent(remoteRepository, k -> ConcurrentHashMap.newKeySet())
                                    .add(ArtifactIdUtils.toId(artifact));
                        } catch (IOException e) {
                            stats.doTransfer(false);
                            throw e;
                        }
                    }

                    @Override
                    public Map<String, String> metadata() {
                        return entry.metadata();
                    }

                    @Override
                    public Map<String, String> checksums() {
                        return entry.checksums();
                    }
                }));
            }
        }
        return stats.doLocate(Optional.empty());
    }

    @Override
    public void store(
            RemoteRepository remoteRepository,
            Artifact artifact,
            Path file,
            Map<String, String> metadata,
            Map<String, String> checksums)
            throws IOException {
        checkState();
        requireNonNull(remoteRepository, "remoteRepository");
        requireNonNull(artifact, "artifact");
        requireNonNull(file, "file");
        requireNonNull(checksums, "checksums");
        if (repositoryPredicate.test(remoteRepository) && artifactPredicate.test(artifact)) {
            URI key = keyMapper.apply(remoteRepository, artifact);
            stats.doStore(Optional.of(localNode.store(key, file, metadata, checksums)));
            storedToCache
                    .computeIfAbsent(remoteRepository, k -> ConcurrentHashMap.newKeySet())
                    .add(ArtifactIdUtils.toId(artifact));
        } else {
            stats.doStore(Optional.empty());
        }
    }

    @Override
    public void mark(RemoteRepository remoteRepository, Artifact artifact, Map<String, String> data)
            throws IOException {
        checkState();
        requireNonNull(remoteRepository, "remoteRepository");
        requireNonNull(artifact, "artifact");
        requireNonNull(data, "data");
        if (repositoryPredicate.test(remoteRepository) && artifactPredicate.test(artifact)) {
            URI key = keyMapper.apply(remoteRepository, artifact);
            localNode.mark(key, data);
        }
    }

    @Override
    public boolean retrievedFromCache(RemoteRepository remoteRepository, Artifact artifact) {
        requireNonNull(remoteRepository, "remoteRepository");
        requireNonNull(artifact, "artifact");
        return retrievedFromCache
                .computeIfAbsent(remoteRepository, k -> ConcurrentHashMap.newKeySet())
                .contains(ArtifactIdUtils.toId(artifact));
    }

    @Override
    public boolean storedToCache(RemoteRepository remoteRepository, Artifact artifact) {
        requireNonNull(remoteRepository, "remoteRepository");
        requireNonNull(artifact, "artifact");
        return storedToCache
                .computeIfAbsent(remoteRepository, k -> ConcurrentHashMap.newKeySet())
                .contains(ArtifactIdUtils.toId(artifact));
    }

    private void checkState() {
        if (closed.get()) {
            throw new IllegalStateException("Session is closed");
        }
    }

    @Override
    public void close() throws IOException {
        if (closed.compareAndSet(false, true)) {
            ArrayList<IOException> exceptions = new ArrayList<>();
            try {
                localNode.close();
            } catch (IOException e) {
                exceptions.add(e);
            }
            if (!exceptions.isEmpty()) {
                IOException closeException = new IOException("Could not close session");
                exceptions.forEach(closeException::addSuppressed);
                throw closeException;
            }
            logger.info("Mimir session closed (RETRIEVED={} CACHED={})", stats.transferSuccess(), stats.storeSuccess());
        }
    }
}
