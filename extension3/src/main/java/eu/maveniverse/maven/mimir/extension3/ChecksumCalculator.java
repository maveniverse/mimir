/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.extension3;

import static java.util.Objects.requireNonNull;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import org.eclipse.aether.spi.connector.checksum.ChecksumAlgorithm;

public class ChecksumCalculator {
    private final Map<String, ChecksumAlgorithm> checksumAlgorithms;

    public ChecksumCalculator(Map<String, ChecksumAlgorithm> checksumAlgorithms) {
        this.checksumAlgorithms = requireNonNull(checksumAlgorithms, "checksumAlgorithms");
    }

    public void update(ByteBuffer buffer) {
        for (ChecksumAlgorithm checksum : checksumAlgorithms.values()) {
            ((Buffer) buffer).mark();
            checksum.update(buffer);
            ((Buffer) buffer).reset();
        }
    }

    public Map<String, String> getChecksums() {
        HashMap<String, String> result = new HashMap<>();
        for (Map.Entry<String, ChecksumAlgorithm> entry : checksumAlgorithms.entrySet()) {
            result.put(entry.getKey(), entry.getValue().checksum());
        }
        return result;
    }
}
