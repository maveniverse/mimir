/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.extension3;

import static java.util.Objects.requireNonNull;

import java.util.concurrent.atomic.AtomicBoolean;
import org.eclipse.aether.transfer.AbstractTransferListener;
import org.eclipse.aether.transfer.TransferEvent;

/**
 * Mimir transfer listener.
 */
public class MimirTransferListener extends AbstractTransferListener {
    private final ChecksumCalculator checksumCalculator;
    private final AtomicBoolean valid;

    public MimirTransferListener(ChecksumCalculator checksumCalculator) {
        this.checksumCalculator = requireNonNull(checksumCalculator, "checksumCalculator");
        this.valid = new AtomicBoolean(true);
    }

    @Override
    public void transferStarted(TransferEvent event) {
        valid.set(event.getTransferredBytes() == 0);
    }

    @Override
    public void transferProgressed(TransferEvent event) {
        if (valid.get()) {
            checksumCalculator.update(event.getDataBuffer());
        }
    }

    @Override
    public void transferFailed(TransferEvent event) {
        valid.set(false);
    }

    @Override
    public void transferCorrupted(TransferEvent event) {
        valid.set(false);
    }

    @Override
    public void transferSucceeded(TransferEvent event) {
        valid.compareAndSet(true, true);
    }

    /**
     * Tells is this transfer "valid": a valid transfer is when it is not resumed download and it succeeded.
     */
    public boolean isValid() {
        return valid.get();
    }
}
