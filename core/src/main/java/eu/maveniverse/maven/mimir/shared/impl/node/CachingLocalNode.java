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
import eu.maveniverse.maven.mimir.shared.node.LocalNode;
import eu.maveniverse.maven.mimir.shared.node.RemoteNode;
import eu.maveniverse.maven.mimir.shared.node.SystemNode;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class CachingLocalNode extends NodeSupport implements LocalNode {
    private final List<LocalNode> localNodes;
    private final SystemNode systemNode;
    private final List<RemoteNode> remoteNodes;

    public CachingLocalNode(List<LocalNode> localNodes, SystemNode systemNode, List<RemoteNode> remoteNodes) {
        super("caching");
        this.localNodes = requireNonNull(localNodes);
        this.systemNode = requireNonNull(systemNode);
        this.remoteNodes = requireNonNull(remoteNodes);
    }

    @Override
    public List<String> checksumAlgorithms() throws IOException {
        return systemNode.checksumAlgorithms();
    }

    @Override
    public Optional<? extends Entry> locate(URI key) throws IOException {
        for (LocalNode node : localNodes) {
            Optional<? extends Entry> localEntry = node.locate(key);
            if (localEntry.isPresent()) {
                return localEntry;
            }
        }
        Entry entry = systemNode.locate(key).orElse(null);
        if (entry == null) {
            for (RemoteNode node : remoteNodes) {
                Optional<? extends Entry> remoteEntry = node.locate(key);
                if (remoteEntry.isPresent()) {
                    entry = systemNode.store(key, remoteEntry.orElseThrow());
                    break;
                }
            }
        }
        return Optional.ofNullable(entry);
    }

    @Override
    public LocalEntry store(URI key, Path file, Map<String, String> metadata, Map<String, String> checksums)
            throws IOException {
        return systemNode.store(key, file, metadata, checksums);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + localNodes + ", " + systemNode + ", " + remoteNodes + ")";
    }
}
