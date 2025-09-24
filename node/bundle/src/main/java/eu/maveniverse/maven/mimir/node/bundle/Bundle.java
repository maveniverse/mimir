/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.node.bundle;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.mimir.shared.naming.Key;
import eu.maveniverse.maven.mimir.shared.node.Entry;
import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * Wrapper around one single bundle, being able to match own origin (container) and serve entries.
 */
public final class Bundle implements Closeable {
    private final String container;
    private final String name;
    private final FileSystem fileSystem;
    private final Path root;
    private final Function<URI, Key> keyResolver;

    public Bundle(String container, String name, FileSystem fileSystem, Function<URI, Key> keyResolver) {
        this.container = requireNonNull(container);
        this.name = requireNonNull(name);
        this.fileSystem = requireNonNull(fileSystem);
        this.root = fileSystem.getPath("/");
        this.keyResolver = requireNonNull(keyResolver);
    }

    public Optional<BundleEntry> locate(URI key) throws IOException {
        Key resolved = keyResolver.apply(key);
        if (Objects.equals(container, resolved.container())) {
            Path path = root.resolve(resolved.name());
            if (Files.isRegularFile(path)) {
                Map<String, String> metadata = loadMetadata(path);
                Map<String, String> checksums = loadChecksums(path);
                return Optional.of(new BundleEntry(metadata, checksums, path));
            }
        }
        return Optional.empty();
    }

    @Override
    public void close() throws IOException {
        fileSystem.close();
    }

    private Map<String, String> loadMetadata(Path file) throws IOException {
        HashMap<String, String> metadata = new HashMap<>();
        metadata.put(
                Entry.CONTENT_LAST_MODIFIED,
                Long.toString(Files.getLastModifiedTime(file).toMillis()));
        metadata.put(Entry.CONTENT_LENGTH, Long.toString(Files.size(file)));
        return metadata;
    }

    private Map<String, String> loadChecksums(Path file) throws IOException {
        HashMap<String, String> checksums = new HashMap<>();
        checksums.put(
                "SHA-1",
                Files.readString(file.getParent().resolve(file.getFileName().toString() + ".sha1")));
        return checksums;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[container=" + container + ", name=" + name + "]";
    }
}
