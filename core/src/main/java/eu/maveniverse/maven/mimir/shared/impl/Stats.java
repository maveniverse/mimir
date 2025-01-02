/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.shared.impl;

import eu.maveniverse.maven.mimir.shared.CacheEntry;
import java.util.Optional;
import java.util.concurrent.atomic.LongAdder;

public final class Stats {
    private final LongAdder queries = new LongAdder();
    private final LongAdder queryHits = new LongAdder();
    private final LongAdder stores = new LongAdder();

    public long queries() {
        return queries.sum();
    }

    public long queryHits() {
        return queryHits.sum();
    }

    public long stores() {
        return stores.sum();
    }

    public Optional<CacheEntry> query(Optional<CacheEntry> entry) {
        queries.increment();
        if (entry.isPresent()) {
            queryHits.increment();
        }
        return entry;
    }

    public CacheEntry store(CacheEntry stored) {
        stores.increment();
        return stored;
    }
}
