/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.shared.impl;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.mimir.shared.CacheEntry;
import eu.maveniverse.maven.mimir.shared.Session;
import eu.maveniverse.maven.mimir.shared.naming.CacheKey;
import eu.maveniverse.maven.mimir.shared.naming.NameMapper;
import eu.maveniverse.maven.mimir.shared.node.LocalNode;
import eu.maveniverse.maven.mimir.shared.node.Node;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.spi.connector.checksum.ChecksumAlgorithmFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SessionImpl implements Session {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final AtomicBoolean closed;
    private final Map<String, ChecksumAlgorithmFactory> checksumAlgorithmFactories;
    private final Predicate<RemoteRepository> repositoryPredicate;
    private final Predicate<Artifact> artifactPredicate;
    private final BiPredicate<RemoteRepository, Artifact> repositoryArtifactPredicate;
    private final NameMapper nameMapper;
    private final LocalNode localNode;
    private final List<Node> nodes;
    private final Stats stats;

    public SessionImpl(
            Map<String, ChecksumAlgorithmFactory> checksumAlgorithmFactories,
            Predicate<RemoteRepository> repositoryPredicate,
            Predicate<Artifact> artifactPredicate,
            NameMapper nameMapper,
            LocalNode localNode,
            List<Node> nodes) {
        this.closed = new AtomicBoolean(false);
        this.checksumAlgorithmFactories = requireNonNull(checksumAlgorithmFactories, "checksumAlgorithmFactories");
        this.repositoryPredicate = requireNonNull(repositoryPredicate, "repositoryPredicate");
        this.artifactPredicate = requireNonNull(artifactPredicate, "artifactPredicate");
        this.repositoryArtifactPredicate = (r, a) -> repositoryPredicate.test(r) && artifactPredicate.test(a);
        this.nameMapper = requireNonNull(nameMapper, "nameMapper");
        this.localNode = requireNonNull(localNode, "localNode");
        this.nodes = requireNonNull(nodes, "nodes");
        this.stats = new Stats();
    }

    @Override
    public boolean repositorySupported(RemoteRepository repository) {
        checkState();
        return repositoryPredicate.test(repository);
    }

    @Override
    public Map<String, ChecksumAlgorithmFactory> checksumFactories() {
        checkState();
        return checksumAlgorithmFactories;
    }

    @Override
    public boolean artifactSupported(RemoteRepository remoteRepository, Artifact artifact) {
        checkState();
        return repositoryArtifactPredicate.test(remoteRepository, artifact);
    }

    @Override
    public Optional<CacheEntry> locate(RemoteRepository remoteRepository, Artifact artifact) throws IOException {
        checkState();
        requireNonNull(remoteRepository, "remoteRepository");
        requireNonNull(artifact, "artifact");
        if (repositoryArtifactPredicate.test(remoteRepository, artifact)) {
            CacheKey key = nameMapper.cacheKey(remoteRepository, artifact);
            Optional<CacheEntry> result = localNode.locate(key);
            if (result.isEmpty()) {
                for (Node node : nodes) {
                    result = node.locate(key);
                    if (result.isPresent()) {
                        result = Optional.of(localNode.store(
                                key, result.orElseThrow(() -> new IllegalStateException("should be present"))));
                        break;
                    }
                }
            }
            return stats.query(result);
        } else {
            return stats.query(Optional.empty());
        }
    }

    @Override
    public boolean store(RemoteRepository remoteRepository, Artifact artifact, Path path, Map<String, String> checksums)
            throws IOException {
        checkState();
        requireNonNull(remoteRepository, "remoteRepository");
        requireNonNull(artifact, "artifact");
        requireNonNull(path, "path");
        requireNonNull(checksums, "checksums");
        if (repositoryArtifactPredicate.test(remoteRepository, artifact)) {
            CacheKey key = nameMapper.cacheKey(remoteRepository, artifact);
            stats.store(localNode.store(key, new PathCacheEntry("session", new PathMetadata(path, checksums), path)));
            return true;
        } else {
            return false;
        }
    }

    private void checkState() {
        if (closed.get()) {
            throw new IllegalStateException("Session is closed");
        }
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            ArrayList<Exception> exceptions = new ArrayList<>();
            for (Node node : this.nodes) {
                try {
                    node.close();
                } catch (Exception e) {
                    exceptions.add(e);
                }
            }
            try {
                localNode.close();
            } catch (Exception e) {
                exceptions.add(e);
            }
            if (!exceptions.isEmpty()) {
                IllegalStateException illegalStateException = new IllegalStateException("Could not close session");
                exceptions.forEach(illegalStateException::addSuppressed);
                throw illegalStateException;
            }
            logger.info(
                    "Mimir session closed (LOCATE/HIT={}/{} STORED={})",
                    stats.queries(),
                    stats.queryHits(),
                    stats.stores());
        }
    }
}
