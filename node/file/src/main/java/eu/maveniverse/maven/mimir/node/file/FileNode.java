/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.node.file;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.mimir.shared.impl.NodeSupport;
import eu.maveniverse.maven.mimir.shared.impl.Utils;
import eu.maveniverse.maven.mimir.shared.naming.Key;
import eu.maveniverse.maven.mimir.shared.node.RemoteEntry;
import eu.maveniverse.maven.mimir.shared.node.SystemNode;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import org.eclipse.aether.spi.connector.checksum.ChecksumAlgorithmFactory;
import org.eclipse.aether.util.FileUtils;

public final class FileNode extends NodeSupport implements SystemNode {
    private final Path basedir;
    private final Function<URI, Key> keyResolver;
    private final List<String> checksumAlgorithms;
    private final Map<String, ChecksumAlgorithmFactory> checksumFactories;

    public FileNode(
            String name,
            Path basedir,
            Function<URI, Key> keyResolver,
            List<String> checksumAlgorithms,
            Map<String, ChecksumAlgorithmFactory> checksumFactories)
            throws IOException {
        super(requireNonNull(name, "name"));
        this.basedir = basedir;
        this.keyResolver = requireNonNull(keyResolver, "keyResolver");
        this.checksumAlgorithms = List.copyOf(checksumAlgorithms);
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
        Path path = resolveKey(key);
        if (Files.isRegularFile(path)) {
            // TODO: hashes
            return Optional.of(FileEntry.createEntry(path, Collections.emptyMap(), Collections.emptyMap()));
        } else {
            return Optional.empty();
        }
    }

    @Override
    public FileEntry store(URI key, RemoteEntry entry) throws IOException {
        ensureOpen();
        Path path = resolveKey(key);
        try (FileUtils.CollocatedTempFile f = FileUtils.newTempFile(path)) {
            entry.handleContent(is -> Files.copy(is, f.getPath()));
            // TODO: ts
            f.move();
        }
        // TODO: hashes
        return FileEntry.createEntry(path, Collections.emptyMap(), Collections.emptyMap());
    }

    @Override
    public void store(URI key, Path file, Map<String, String> checksums) throws IOException {
        ensureOpen();
        Path path = resolveKey(key);
        try (FileUtils.CollocatedTempFile f = FileUtils.newTempFile(path)) {
            Utils.copyOrLink(file, f.getPath());
            // TODO: hashes
            f.move();
        }
    }

    private Path resolveKey(URI key) {
        Key resolved = keyResolver.apply(key);
        return basedir.resolve(resolved.container()).resolve(resolved.name());
    }

    @Override
    public String toString() {
        return name + " (basedir=" + basedir + ")";
    }
}
