/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.shared.impl.file;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.mimir.shared.impl.NodeSupport;
import eu.maveniverse.maven.mimir.shared.impl.Utils;
import eu.maveniverse.maven.mimir.shared.naming.KeyResolver;
import eu.maveniverse.maven.mimir.shared.node.Entry;
import eu.maveniverse.maven.mimir.shared.node.SystemNode;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import org.eclipse.aether.spi.connector.checksum.ChecksumAlgorithmFactory;
import org.eclipse.aether.util.FileUtils;

public final class FileNode extends NodeSupport implements SystemNode {
    private final Path basedir;
    private final BiFunction<Path, URI, Path> keyResolver;
    private final List<String> checksumAlgorithms;
    private final Map<String, ChecksumAlgorithmFactory> checksumFactories;

    public FileNode(
            String name,
            int distance,
            Path basedir,
            KeyResolver keyResolver,
            List<String> checksumAlgorithms,
            Map<String, ChecksumAlgorithmFactory> checksumFactories)
            throws IOException {
        super(requireNonNull(name, "name"), distance);
        this.basedir = basedir;
        this.keyResolver = requireNonNull(keyResolver, "keyResolver");
        this.checksumAlgorithms = List.copyOf(checksumFactories.keySet());
        this.checksumFactories = Map.copyOf(checksumFactories);

        Files.createDirectories(basedir);
    }

    @Override
    public List<String> checksumAlgorithms() {
        return checksumAlgorithms;
    }

    @Override
    public Map<String, ChecksumAlgorithmFactory> checksumFactories() {
        return checksumFactories;
    }

    @Override
    public Optional<FileEntry> locate(URI key) throws IOException {
        ensureOpen();
        Path path = keyResolver.apply(basedir, key);
        if (Files.isRegularFile(path)) {
            // TODO: hashes
            return Optional.of(FileEntry.createEntry(path, Collections.emptyMap(), Collections.emptyMap()));
        } else {
            return Optional.empty();
        }
    }

    @Override
    public FileEntry store(URI key, Entry entry) throws IOException {
        ensureOpen();
        Path path = keyResolver.apply(basedir, key);
        try (FileUtils.CollocatedTempFile f = FileUtils.newTempFile(path)) {
            entry.transferTo(f.getPath());
            f.move();
        }
        // TODO: hashes
        return FileEntry.createEntry(path, Collections.emptyMap(), Collections.emptyMap());
    }

    @Override
    public void store(URI key, Path file, Map<String, String> checksums) throws IOException {
        ensureOpen();
        Path path = keyResolver.apply(basedir, key);
        try (FileUtils.CollocatedTempFile f = FileUtils.newTempFile(path)) {
            Utils.copyOrLink(file, f.getPath());
            // TODO: hashes
            f.move();
        }
    }

    @Override
    public String toString() {
        return name + " (distance=" + distance + " basedir=" + basedir + ")";
    }
}
