/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.shared.impl.node;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.mimir.shared.node.Entry;
import eu.maveniverse.maven.mimir.shared.node.LocalEntry;
import eu.maveniverse.maven.mimir.shared.node.RemoteEntry;
import eu.maveniverse.maven.mimir.shared.node.RemoteNode;
import eu.maveniverse.maven.mimir.shared.node.SystemNode;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A wrapper system node that performs caching from remote nodes into given system node, if system node
 * does not have content for asked key.
 */
public class CachingSystemNode extends NodeSupport implements SystemNode {
    private final SystemNode systemNode;
    private final List<RemoteNode> remoteNodes;

    public CachingSystemNode(SystemNode systemNode, List<RemoteNode> remoteNodes) {
        super("caching");
        this.systemNode = requireNonNull(systemNode);
        this.remoteNodes = requireNonNull(remoteNodes);
    }

    @Override
    public List<String> checksumAlgorithms() throws IOException {
        return systemNode.checksumAlgorithms();
    }

    @Override
    public Optional<? extends LocalEntry> locate(URI key) throws IOException {
        Optional<? extends LocalEntry> entry = systemNode.locate(key);
        if (entry.isPresent()) {
            return entry;
        } else {
            for (RemoteNode node : remoteNodes) {
                Optional<? extends RemoteEntry> remoteEntry = node.locate(key);
                if (remoteEntry.isPresent()) {
                    return Optional.of(systemNode.store(key, remoteEntry.orElseThrow()));
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public LocalEntry store(URI key, Path file, Map<String, String> metadata, Map<String, String> checksums)
            throws IOException {
        return systemNode.store(key, file, metadata, checksums);
    }

    @Override
    public LocalEntry store(URI key, Entry entry) throws IOException {
        return systemNode.store(key, entry);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + systemNode + ", " + remoteNodes + ")";
    }
}
