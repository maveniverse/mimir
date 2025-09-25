/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.shared.impl;

import eu.maveniverse.maven.mimir.shared.node.Entry;
import java.util.HashMap;
import java.util.Map;

/**
 * Utilities related to handling {@link Entry} and metadata.
 */
public final class EntryUtils {
    private EntryUtils() {}

    private static final String METADATA_PREFIX = "m.";
    private static final String CHECKSUM_PREFIX = "c.";

    public static Map<String, String> mergeEntry(Entry entry) {
        return mergeEntry(entry.metadata(), entry.checksums());
    }

    public static Map<String, String> mergeEntry(Map<String, String> metadata, Map<String, String> checksums) {
        HashMap<String, String> merged = new HashMap<>();
        metadata.forEach((k, v) -> merged.put(METADATA_PREFIX + k, v));
        checksums.forEach((k, v) -> merged.put(CHECKSUM_PREFIX + k, v));
        return merged;
    }

    private static Map<String, String> split(Map<String, String> merged, String prefix) {
        HashMap<String, String> result = new HashMap<>();
        merged.entrySet().stream()
                .filter(e -> e.getKey().startsWith(prefix))
                .forEach(e -> result.put(e.getKey().substring(prefix.length()), e.getValue()));
        return result;
    }

    public static Map<String, String> splitMetadata(Map<String, String> merged) {
        return split(merged, METADATA_PREFIX);
    }

    public static Map<String, String> splitChecksums(Map<String, String> merged) {
        return split(merged, CHECKSUM_PREFIX);
    }
}
