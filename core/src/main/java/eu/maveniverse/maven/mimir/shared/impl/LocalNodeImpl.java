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
import eu.maveniverse.maven.mimir.shared.node.LocalCacheEntry;
import eu.maveniverse.maven.mimir.shared.node.LocalNode;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Optional;
import org.eclipse.aether.util.FileUtils;

public final class LocalNodeImpl implements LocalNode {
    private final LocalNodeConfig config;

    public LocalNodeImpl(LocalNodeConfig config) throws IOException {
        this.config = requireNonNull(config, "config");
        Files.createDirectories(config.basedir());
    }

    @Override
    public String name() {
        return config.name();
    }

    @Override
    public int distance() {
        return config.distance();
    }

    @Override
    public Optional<CacheEntry> locate(CacheKey key) {
        Path path = resolve(key);
        if (Files.isRegularFile(path)) {
            return Optional.of(new LocalCacheEntryImpl(config.name(), path));
        } else {
            return Optional.empty();
        }
    }

    @Override
    public Path basedir() {
        return config.basedir();
    }

    @Override
    public LocalCacheEntry store(CacheKey key, Path content) throws IOException {
        Path path = resolve(key);
        try (FileUtils.CollocatedTempFile f = FileUtils.newTempFile(path)) {
            Utils.copyOrLink(content, f.getPath());
            f.move();
        }
        return new LocalCacheEntryImpl(config.name(), path);
    }

    @Override
    public LocalCacheEntry store(CacheKey key, CacheEntry entry) throws IOException {
        Path path = resolve(key);
        try (FileUtils.CollocatedTempFile f = FileUtils.newTempFile(path)) {
            entry.transferTo(f.getPath());
            f.move();
        }
        return new LocalCacheEntryImpl(entry.origin(), path);
    }

    @Override
    public void close() {}

    private Path resolve(CacheKey key) {
        return config.basedir().resolve(key.container()).resolve(key.name());
    }

    private static final class LocalCacheEntryImpl implements LocalCacheEntry {
        private final String origin;
        private final Path cacheFile;

        private LocalCacheEntryImpl(String origin, Path cacheFile) {
            this.origin = origin;
            this.cacheFile = cacheFile;
        }

        @Override
        public String origin() {
            return origin;
        }

        @Override
        public void transferTo(Path file) throws IOException {
            Files.deleteIfExists(file);
            try (FileUtils.CollocatedTempFile f = FileUtils.newTempFile(file)) {
                Utils.copyOrLink(cacheFile, f.getPath());
                f.move();
            }
        }

        @Override
        public FileChannel getReadFileChannel() throws IOException {
            return FileChannel.open(cacheFile, StandardOpenOption.READ);
        }
    }
}
