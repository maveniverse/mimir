/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.shared.impl.file;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.mimir.shared.impl.EntrySupport;
import eu.maveniverse.maven.mimir.shared.impl.NodeSupport;
import eu.maveniverse.maven.mimir.shared.impl.Utils;
import eu.maveniverse.maven.mimir.shared.naming.KeyResolver;
import eu.maveniverse.maven.mimir.shared.node.Entry;
import eu.maveniverse.maven.mimir.shared.node.LocalEntry;
import eu.maveniverse.maven.mimir.shared.node.LocalNode;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import org.eclipse.aether.spi.connector.checksum.ChecksumAlgorithmFactory;
import org.eclipse.aether.util.FileUtils;

public final class FileNode extends NodeSupport implements LocalNode {
    private final Path basedir;
    private final BiFunction<Path, URI, Path> keyResolver;
    private final Map<String, ChecksumAlgorithmFactory> checksumFactories;

    public FileNode(
            String name,
            int distance,
            Path basedir,
            KeyResolver keyResolver,
            Map<String, ChecksumAlgorithmFactory> checksumFactories)
            throws IOException {
        super(requireNonNull(name, "name"), distance);
        this.basedir = basedir;
        this.keyResolver = requireNonNull(keyResolver, "keyResolver");
        this.checksumFactories = Map.copyOf(requireNonNull(checksumFactories, "checksumFactories"));

        Files.createDirectories(basedir);
    }

    @Override
    public Map<String, ChecksumAlgorithmFactory> checksumFactories() {
        return checksumFactories;
    }

    @Override
    public Optional<LocalEntry> locate(URI key) throws IOException {
        ensureOpen();
        Path path = resolve(key);
        if (Files.isRegularFile(path)) {
            return Optional.of(createEntry(path));
        } else {
            return Optional.empty();
        }
    }

    @Override
    public LocalEntry store(URI key, Entry entry) throws IOException {
        ensureOpen();
        Path path = resolve(key);
        try (FileUtils.CollocatedTempFile f = FileUtils.newTempFile(path)) {
            entry.transferTo(f.getPath());
            f.move();
        }
        return createEntry(path);
    }

    @Override
    public LocalEntry store(URI key, Path file, Map<String, String> checksums) throws IOException {
        ensureOpen();
        Path path = resolve(key);
        try (FileUtils.CollocatedTempFile f = FileUtils.newTempFile(path)) {
            Utils.copyOrLink(file, f.getPath());
            f.move();
        }
        return createEntry(path);
    }

    @Override
    public String toString() {
        return name + " (distance=" + distance + " basedir=" + basedir + ")";
    }

    InputStream openStream(Path path) throws IOException {
        ensureOpen();
        return Files.newInputStream(path);
    }

    void transferTo(Path src, Path dst) throws IOException {
        ensureOpen();
        Files.deleteIfExists(dst);
        try (FileUtils.CollocatedTempFile f = FileUtils.newTempFile(dst)) {
            Utils.copyOrLink(src, f.getPath());
            f.move();
        }
    }

    private Path resolve(URI key) {
        return keyResolver.apply(basedir, key);
    }

    private FileEntryImpl createEntry(Path file) throws IOException {
        HashMap<String, String> metadata = new HashMap<>();
        metadata.put(Entry.CONTENT_LENGTH, Long.toString(Files.size(file)));
        metadata.put(
                Entry.CONTENT_LAST_MODIFIED,
                Long.toString(Files.getLastModifiedTime(file).toMillis()));
        return new FileEntryImpl(this, metadata, file);
    }

    private static class FileEntryImpl extends EntrySupport implements LocalEntry {
        private final Path path;
        private final FileNode localNode;

        public FileEntryImpl(FileNode origin, Map<String, String> metadata, Path path) {
            super(origin, metadata);
            this.path = requireNonNull(path, "path");
            this.localNode = origin;
        }

        @Override
        public InputStream openStream() throws IOException {
            return localNode.openStream(path);
        }

        @Override
        public void transferTo(Path file) throws IOException {
            localNode.transferTo(path, file);
        }
    }
}
