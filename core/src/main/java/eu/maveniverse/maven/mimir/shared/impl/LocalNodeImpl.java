/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.shared.impl;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.mimir.shared.node.Entry;
import eu.maveniverse.maven.mimir.shared.node.Key;
import eu.maveniverse.maven.mimir.shared.node.LocalEntry;
import eu.maveniverse.maven.mimir.shared.node.LocalNode;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.eclipse.aether.util.FileUtils;

public final class LocalNodeImpl extends NodeSupport implements LocalNode {
    private final LocalNodeConfig config;

    public LocalNodeImpl(LocalNodeConfig config) throws IOException {
        super(requireNonNull(config, "config").name(), config.distance());
        this.config = config;
        Files.createDirectories(config.basedir());
    }

    @Override
    public Optional<Entry> locate(Key key) throws IOException {
        ensureOpen();
        Path path = resolve(key);
        if (Files.isRegularFile(path)) {
            return Optional.of(createEntry(path));
        } else {
            return Optional.empty();
        }
    }

    @Override
    public LocalEntry store(Key key, Entry entry) throws IOException {
        ensureOpen();
        Path path = resolve(key);
        try (FileUtils.CollocatedTempFile f = FileUtils.newTempFile(path)) {
            entry.transferTo(f.getPath());
            f.move();
        }
        return createEntry(path);
    }

    @Override
    public LocalEntry store(Key key, Path file, Map<String, String> checksums) throws IOException {
        ensureOpen();
        Path path = resolve(key);
        try (FileUtils.CollocatedTempFile f = FileUtils.newTempFile(path)) {
            Utils.copyOrLink(file, f.getPath());
            f.move();
        }
        return createEntry(path);
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

    private Path resolve(Key key) {
        return config.basedir().resolve(key.container()).resolve(key.name());
    }

    private LocalEntryImpl createEntry(Path file) throws IOException {
        HashMap<String, String> metadata = new HashMap<>();
        metadata.put(Entry.CONTENT_LENGTH, Long.toString(Files.size(file)));
        metadata.put(
                Entry.CONTENT_LAST_MODIFIED,
                Long.toString(Files.getLastModifiedTime(file).toMillis()));
        return new LocalEntryImpl(this, metadata, file);
    }

    private static class LocalEntryImpl extends EntrySupport implements LocalEntry {
        private final Path path;
        private final LocalNodeImpl localNode;

        public LocalEntryImpl(LocalNodeImpl origin, Map<String, String> metadata, Path path) {
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
