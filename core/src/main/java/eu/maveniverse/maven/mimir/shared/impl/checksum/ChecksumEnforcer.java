/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.shared.impl.checksum;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public final class ChecksumEnforcer implements Consumer<Map<String, String>> {
    /**
     * Exception thrown by checksum enforcer.
     */
    public static class ChecksumEnforcerException extends IOException {
        private final Map<String, String> expected;
        private final Map<String, String> received;

        public ChecksumEnforcerException(String message, Map<String, String> expected, Map<String, String> received) {
            super(message);
            this.expected = requireNonNull(expected);
            this.received = requireNonNull(received);
        }

        public Map<String, String> getExpected() {
            return expected;
        }

        public Map<String, String> getReceived() {
            return received;
        }
    }

    private final Map<String, String> expectedChecksums;
    private final AtomicReference<Map<String, String>> checksums;

    public ChecksumEnforcer(Map<String, String> expectedChecksums) {
        this.expectedChecksums = Map.copyOf(requireNonNull(expectedChecksums, "expectedChecksums"));
        this.checksums = new AtomicReference<>();
    }

    @Override
    public void accept(Map<String, String> checksums) {
        this.checksums.set(Map.copyOf(requireNonNull(checksums, "checksums")));
        int matches = 0;
        for (Map.Entry<String, String> expectedEntry : expectedChecksums.entrySet()) {
            String checksum = checksums.get(expectedEntry.getKey());
            if (checksum != null) {
                if (checksum.equals(expectedEntry.getValue())) {
                    matches++;
                } else {
                    throw new UncheckedIOException(new ChecksumEnforcerException(
                            String.format(
                                    "Checksum %s does not match expected value %s", checksum, expectedEntry.getValue()),
                            expectedChecksums,
                            checksums));
                }
            }
        }
        if (matches == 0) {
            throw new UncheckedIOException(new ChecksumEnforcerException(
                    String.format(
                            "No checksum algorithm intersection exists: expected %s vs calculated %s",
                            expectedChecksums.keySet(), checksums.keySet()),
                    expectedChecksums,
                    checksums));
        }
    }

    public Map<String, String> getExpectedChecksums() {
        return expectedChecksums;
    }

    public Map<String, String> getChecksums() {
        return checksums.get();
    }
}
