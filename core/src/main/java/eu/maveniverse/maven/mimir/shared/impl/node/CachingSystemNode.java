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
import eu.maveniverse.maven.mimir.shared.node.SystemEntry;
import eu.maveniverse.maven.mimir.shared.node.SystemNode;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class CachingSystemNode extends NodeSupport implements SystemNode {
    private final List<LocalNode> localNodes;
    private final SystemNode systemNode;
    private final List<RemoteNode> remoteNodes;

    public CachingSystemNode(List<LocalNode> localNodes, SystemNode systemNode, List<RemoteNode> remoteNodes) {
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
    public SystemEntry store(URI key, Path file, Map<String, String> metadata, Map<String, String> checksums)
            throws IOException {
        return systemNode.store(key, file, metadata, checksums);
    }

    @Override
    public SystemEntry store(URI key, Entry entry) throws IOException {
        return systemNode.store(key, entry);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + localNodes + ", " + systemNode + ", " + remoteNodes + ")";
    }

    private static class SE implements SystemEntry {
        private final LocalEntry localEntry;

        private SE(LocalEntry localEntry) {
            this.localEntry = localEntry;
        }

        @Override
        public InputStream inputStream() throws IOException {
            if (localEntry instanceof SystemEntry se) {
                return se.inputStream();
            }
            throw new IOException("this is local entry in disguise");
        }

        @Override
        public void transferTo(Path file) throws IOException {
            localEntry.transferTo(file);
        }

        @Override
        public Map<String, String> metadata() {
            return localEntry.metadata();
        }

        @Override
        public Map<String, String> checksums() {
            return localEntry.checksums();
        }
    }
}
