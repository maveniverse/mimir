/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.shared.impl;

import java.util.concurrent.atomic.LongAdder;

public final class Stats {
    private final LongAdder locate = new LongAdder();
    private final LongAdder locateHit = new LongAdder();
    private final LongAdder store = new LongAdder();
    private final LongAdder storeAccepted = new LongAdder();

    public long locate() {
        return locate.sum();
    }

    public long locateHit() {
        return locateHit.sum();
    }

    public long store() {
        return store.sum();
    }

    public long storeAccepted() {
        return storeAccepted.sum();
    }

    public void query(boolean present) {
        locate.increment();
        if (present) {
            locateHit.increment();
        }
    }

    public void store(boolean accepted) {
        store.increment();
        if (accepted) {
            storeAccepted.increment();
        }
    }
}
