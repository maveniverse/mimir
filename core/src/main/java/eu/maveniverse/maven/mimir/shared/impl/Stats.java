/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.shared.impl;

import java.util.Optional;
import java.util.concurrent.atomic.LongAdder;

public final class Stats {
    private final LongAdder locate = new LongAdder();
    private final LongAdder locateSuccess = new LongAdder();
    private final LongAdder transfer = new LongAdder();
    private final LongAdder transferSuccess = new LongAdder();
    private final LongAdder store = new LongAdder();
    private final LongAdder storeSuccess = new LongAdder();

    public long locate() {
        return locate.sum();
    }

    public long locateSuccess() {
        return locateSuccess.sum();
    }

    public long transfer() {
        return transfer.sum();
    }

    public long transferSuccess() {
        return transferSuccess.sum();
    }

    public long store() {
        return store.sum();
    }

    public long storeSuccess() {
        return storeSuccess.sum();
    }

    public <T> Optional<T> doLocate(Optional<T> optional) {
        locate.increment();
        if (optional.isPresent()) {
            locateSuccess.increment();
        }
        return optional;
    }

    public void doTransfer(boolean success) {
        transfer.increment();
        if (success) {
            transferSuccess.increment();
        }
    }

    public <T> Optional<T> doStore(Optional<T> optional) {
        store.increment();
        if (optional.isPresent()) {
            storeSuccess.increment();
        }
        return optional;
    }
}
