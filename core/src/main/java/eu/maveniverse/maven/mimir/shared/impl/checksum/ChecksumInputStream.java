/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.shared.impl.checksum;

import static java.util.Objects.requireNonNull;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import org.eclipse.aether.spi.connector.checksum.ChecksumAlgorithm;

public final class ChecksumInputStream extends FilterInputStream {
    private final Map<String, ChecksumAlgorithm> checksumAlgorithms;
    private final Consumer<Map<String, String>> checksumsCallback;

    public ChecksumInputStream(
            InputStream inputStream,
            Map<String, ChecksumAlgorithm> checksumAlgorithms,
            Consumer<Map<String, String>> checksumsCallback) {
        super(requireNonNull(inputStream, "inputStream"));
        this.checksumAlgorithms = new HashMap<>(requireNonNull(checksumAlgorithms, "checksumAlgorithms"));
        this.checksumsCallback = checksumsCallback;
    }

    @Override
    public int read() throws IOException {
        int b = super.read();
        if (b != -1) {
            ByteBuffer buffer = ByteBuffer.allocate(1);
            buffer.put((byte) b);
            for (ChecksumAlgorithm checksumAlgorithm : checksumAlgorithms.values()) {
                buffer.rewind();
                checksumAlgorithm.update(buffer);
            }
        }
        return b;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int res = super.read(b, off, len);
        if (res != -1) {
            ByteBuffer buffer = ByteBuffer.wrap(b, off, res);
            for (ChecksumAlgorithm checksumAlgorithm : checksumAlgorithms.values()) {
                buffer.rewind();
                checksumAlgorithm.update(buffer);
            }
        }
        return res;
    }

    @Override
    public void mark(int readLimit) {}

    @Override
    public void reset() throws IOException {
        throw new IOException("mark/reset not supported");
    }

    @Override
    public boolean markSupported() {
        return false;
    }

    @Override
    public void close() throws IOException {
        super.close();
        try {
            HashMap<String, String> result = new HashMap<>();
            for (Map.Entry<String, ChecksumAlgorithm> entry : checksumAlgorithms.entrySet()) {
                result.put(entry.getKey(), entry.getValue().checksum());
            }
            checksumsCallback.accept(result);
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
    }
}
