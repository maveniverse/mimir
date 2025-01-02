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
import eu.maveniverse.maven.mimir.shared.CacheKey;
import eu.maveniverse.maven.mimir.shared.Session;
import eu.maveniverse.maven.mimir.shared.naming.NameMapper;
import eu.maveniverse.maven.mimir.shared.node.LocalNode;
import eu.maveniverse.maven.mimir.shared.node.Node;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SessionImpl implements Session {
    private final Logger logger = LoggerFactory.getLogger(SessionImpl.class);
    private final NameMapper nameMapper;
    private final LocalNode localNode;
    private final List<Node> nodes;
    private final Stats stats;

    public SessionImpl(NameMapper nameMapper, LocalNode localNode, List<Node> nodes) {
        this.nameMapper = requireNonNull(nameMapper);
        this.localNode = requireNonNull(localNode);
        this.nodes = requireNonNull(nodes);
        this.stats = new Stats();
    }

    @Override
    public boolean supports(RemoteRepository repository) {
        // for now, we do only "real remote" artifact caching, those coming over HTTP only (but this includes S3/minio)
        String protocol = repository.getProtocol();
        return protocol != null && protocol.contains("http");
    }

    @Override
    public CacheKey cacheKey(RemoteRepository remoteRepository, Artifact artifact) {
        if (!supports(remoteRepository)) {
            throw new IllegalArgumentException("Unsupported remote repository: " + remoteRepository);
        }
        return nameMapper.cacheKey(remoteRepository, artifact);
    }

    @Override
    public Optional<CacheEntry> locate(CacheKey key) throws IOException {
        requireNonNull(key, "key");
        Optional<CacheEntry> result = localNode.locate(key);
        if (!result.isPresent()) {
            for (Node node : this.nodes) {
                result = node.locate(key);
                if (result.isPresent()) {
                    break;
                }
            }
        }
        return stats.query(result);
    }

    @Override
    public boolean store(CacheKey key, Path content) throws IOException {
        requireNonNull(key, "key");
        requireNonNull(content, "content");
        if (!Files.isRegularFile(content)) {
            throw new IllegalArgumentException("Not a regular file: " + content);
        }
        return stats.store(localNode.store(key, content));
    }

    @Override
    public void close() {
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
        if (logger.isDebugEnabled()) {
            logger.debug(
                    "Session stats: Q={}/{} S={}/{}",
                    stats.queries(),
                    stats.queryHits(),
                    stats.stores(),
                    stats.storesHits());
        }
    }
}
