/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.shared.impl;

import eu.maveniverse.maven.mimir.shared.CacheEntry;
import eu.maveniverse.maven.mimir.shared.CacheKey;
import eu.maveniverse.maven.mimir.shared.node.LocalCacheEntry;
import eu.maveniverse.maven.mimir.shared.node.LocalNode;
import eu.maveniverse.maven.mimir.shared.util.FileUtils;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public final class LocalNodeImpl implements LocalNode {
    private final String name;
    private final int distance;
    private final Path basedir;

    public LocalNodeImpl(String name, int distance, Path basedir) {
        this.name = name;
        this.distance = distance;
        this.basedir = basedir;
    }

    @Override
    public String id() {
        return name;
    }

    @Override
    public int distance() {
        return distance;
    }

    @Override
    public Optional<CacheEntry> locate(CacheKey key) {
        Path path = resolve(key);
        if (Files.isRegularFile(path)) {
            return Optional.of(new LocalCacheEntryImpl(path));
        } else {
            return Optional.empty();
        }
    }

    @Override
    public LocalCacheEntry store(CacheKey key, Path content) throws IOException {
        Path path = resolve(key);
        try (FileUtils.CollocatedTempFile f = FileUtils.newTempFile(path, false)) {
            Utils.copyOrLink(content, f.getPath());
            f.move();
        }
        return new LocalCacheEntryImpl(path);
    }

    @Override
    public LocalCacheEntry store(CacheKey key, CacheEntry entry) throws IOException {
        Path path = resolve(key);
        try (FileUtils.CollocatedTempFile f = FileUtils.newTempFile(path, false)) {
            entry.transferTo(f.getPath());
            f.move();
        }
        return new LocalCacheEntryImpl(path);
    }

    @Override
    public void close() {}

    private Path resolve(CacheKey key) {
        return basedir.resolve(key.container()).resolve(key.name());
    }

    public static final class LocalCacheEntryImpl implements LocalCacheEntry {
        private final Path cacheFile;

        private LocalCacheEntryImpl(Path cacheFile) {
            this.cacheFile = cacheFile;
        }

        @Override
        public void transferTo(Path file) throws IOException {
            Files.deleteIfExists(file);
            try (FileUtils.CollocatedTempFile f = FileUtils.newTempFile(file, false)) {
                Utils.copyOrLink(cacheFile, f.getPath());
                f.move();
            }
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return new BufferedInputStream(Files.newInputStream(cacheFile));
        }
    }
}
