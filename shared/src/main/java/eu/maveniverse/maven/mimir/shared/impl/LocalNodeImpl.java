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
import eu.maveniverse.maven.mimir.shared.node.LocalNode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public final class LocalNodeImpl implements LocalNode {
    private final Path basedir;

    public LocalNodeImpl(Path basedir) {
        this.basedir = basedir;
    }

    @Override
    public String id() {
        return "local";
    }

    @Override
    public int distance() {
        return 0;
    }

    @Override
    public Optional<CacheEntry> locate(CacheKey key) {
        Path path = resolve(key);
        if (Files.isRegularFile(path)) {
            return Optional.of(new LocalCacheEntry(path));
        } else {
            return Optional.empty();
        }
    }

    @Override
    public boolean store(CacheKey key, Path content) throws IOException {
        Path path = resolve(key);
        Files.createDirectories(path.getParent());
        Files.deleteIfExists(path);
        Files.createLink(path, content);
        Files.setLastModifiedTime(path, Files.getLastModifiedTime(content));
        return true;
    }

    @Override
    public void close() {}

    private Path resolve(CacheKey key) {
        if (key.name().c().isPresent()) {
            return basedir.resolve(key.bucket())
                    .resolve(key.name().g())
                    .resolve(key.name().a())
                    .resolve(key.name().v())
                    .resolve(key.name().a() + "-" + key.name().v() + "-"
                            + key.name().c().get() + "." + key.name().e());
        } else {
            return basedir.resolve(key.bucket())
                    .resolve(key.name().g())
                    .resolve(key.name().a())
                    .resolve(key.name().v())
                    .resolve(key.name().a() + "-" + key.name().v() + "."
                            + key.name().e());
        }
    }

    private static final class LocalCacheEntry implements CacheEntry {
        private final Path file;

        public LocalCacheEntry(Path file) {
            this.file = file;
        }

        @Override
        public void transferTo(Path file) throws IOException {
            Files.createDirectories(file.getParent());
            Files.deleteIfExists(file);
            Files.createLink(file, this.file);
        }
    }
}
