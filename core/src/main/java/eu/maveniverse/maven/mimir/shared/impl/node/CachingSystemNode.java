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
import eu.maveniverse.maven.mimir.shared.node.SystemEntry;
import eu.maveniverse.maven.mimir.shared.node.SystemNode;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.eclipse.aether.spi.connector.checksum.ChecksumAlgorithmFactory;

public final class CachingSystemNode implements SystemNode {
    private final SystemNode one;
    private final SystemNode two;

    public CachingSystemNode(SystemNode one, SystemNode two) {
        this.one = requireNonNull(one, "one");
        this.two = requireNonNull(two, "two");
    }

    @Override
    public List<String> checksumAlgorithms() throws IOException {
        return one.checksumAlgorithms();
    }

    @Override
    public Map<String, ChecksumAlgorithmFactory> checksumFactories() throws IOException {
        return one.checksumFactories();
    }

    @Override
    public Optional<? extends SystemEntry> locate(URI key) throws IOException {
        Optional<? extends SystemEntry> entry = one.locate(key);
        if (entry.isEmpty()) {
            entry = two.locate(key);
            if (entry.isPresent()) {
                entry = Optional.of(one.store(key, entry.orElseThrow()));
            }
        }
        return entry;
    }

    @Override
    public SystemEntry store(URI key, Entry remoteEntry) throws IOException {
        return one.store(key, remoteEntry);
    }

    @Override
    public void store(URI key, Path file, Map<String, String> checksums) throws IOException {
        one.store(key, file, checksums);
    }

    @Override
    public void close() throws IOException {
        one.close();
        two.close();
    }

    @Override
    public String name() {
        return getClass().getSimpleName() + "()";
    }
}
