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
import eu.maveniverse.maven.mimir.shared.node.Entry;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

public final class CacheEntryImpl implements CacheEntry {
    private final Entry entry;
    private final CacheEntry.Metadata metadata;

    public CacheEntryImpl(Entry entry) {
        this.entry = requireNonNull(entry, "entry");
        this.metadata = new Metadata() {
            @Override
            public long contentLength() {
                return Long.parseLong(requireNonNull(entry.metadata().get(Entry.CONTENT_LENGTH), Entry.CONTENT_LENGTH));
            }

            @Override
            public Optional<Instant> lastModified() {
                String contentLastModified = entry.metadata().get(Entry.CONTENT_LAST_MODIFIED);
                if (contentLastModified == null) {
                    return Optional.empty();
                }
                return Optional.of(Instant.ofEpochMilli(Long.parseLong(contentLastModified)));
            }

            @Override
            public Map<String, String> checksums() {
                return Map.of();
            }
        };
    }

    @Override
    public Metadata metadata() {
        return metadata;
    }

    @Override
    public void transferTo(Path file) throws IOException {
        entry.transferTo(file);
    }
}
