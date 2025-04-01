/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.shared.node;

import static java.util.Objects.requireNonNull;

import java.time.Instant;
import java.util.Map;

public interface Entry {
    String CONTENT_PREFIX = "content-";
    String CONTENT_LENGTH = CONTENT_PREFIX + "length";
    String CONTENT_LAST_MODIFIED = CONTENT_PREFIX + "modified";

    /**
     * The entry metadata.
     */
    Map<String, String> metadata();

    /**
     * Entry checksums.
     */
    Map<String, String> checksums();

    default long getContentLength() {
        return Long.parseLong(requireNonNull(metadata().get(Entry.CONTENT_LENGTH), Entry.CONTENT_LENGTH));
    }

    static void setContentLength(Map<String, String> metadata, long contentLength) {
        requireNonNull(metadata, "metadata cannot be null");
        if (contentLength < 0) {
            throw new IllegalArgumentException("Content length cannot be negative");
        }
        metadata.put(Entry.CONTENT_LENGTH, Long.toString(contentLength));
    }

    default Instant getContentLastModified() {
        return Instant.ofEpochMilli(Long.parseLong(
                requireNonNull(metadata().get(Entry.CONTENT_LAST_MODIFIED), Entry.CONTENT_LAST_MODIFIED)));
    }

    static void setContentLastModified(Map<String, String> metadata, Instant contentLastModified) {
        requireNonNull(metadata, "metadata cannot be null");
        requireNonNull(contentLastModified, "Content last modified cannot be null");
        metadata.put(Entry.CONTENT_LAST_MODIFIED, Long.toString(contentLastModified.toEpochMilli()));
    }
}
