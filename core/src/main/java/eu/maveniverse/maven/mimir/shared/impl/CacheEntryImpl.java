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
import java.util.Map;

public final class CacheEntryImpl implements CacheEntry {
    private final Entry entry;

    public CacheEntryImpl(Entry entry) {
        this.entry = requireNonNull(entry, "entry");
    }

    @Override
    public Map<String, String> checksums() {
        return entry.checksums();
    }

    @Override
    public void transferTo(Path file) throws IOException {
        entry.transferTo(file);
    }
}
