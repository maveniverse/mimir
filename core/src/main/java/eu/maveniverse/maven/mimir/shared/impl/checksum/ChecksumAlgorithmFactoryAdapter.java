/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.shared.impl.checksum;

import eu.maveniverse.maven.mimir.shared.checksum.ChecksumAlgorithm;
import eu.maveniverse.maven.mimir.shared.checksum.ChecksumAlgorithmFactory;
import java.nio.ByteBuffer;

public class ChecksumAlgorithmFactoryAdapter implements ChecksumAlgorithmFactory {
    private final org.eclipse.aether.spi.connector.checksum.ChecksumAlgorithmFactory delegate;

    public ChecksumAlgorithmFactoryAdapter(
            org.eclipse.aether.spi.connector.checksum.ChecksumAlgorithmFactory delegate) {
        this.delegate = delegate;
    }

    @Override
    public String getName() {
        return delegate.getName();
    }

    @Override
    public String getFileExtension() {
        return delegate.getFileExtension();
    }

    @Override
    public ChecksumAlgorithm getAlgorithm() {
        org.eclipse.aether.spi.connector.checksum.ChecksumAlgorithm delegateAlgorithm = delegate.getAlgorithm();
        return new ChecksumAlgorithm() {
            @Override
            public ChecksumAlgorithmFactory factory() {
                return ChecksumAlgorithmFactoryAdapter.this;
            }

            @Override
            public void update(ByteBuffer input) {
                delegateAlgorithm.update(input);
            }

            @Override
            public void update(byte[] input) {
                delegateAlgorithm.update(ByteBuffer.wrap(input));
            }

            @Override
            public String checksum() {
                return delegateAlgorithm.checksum();
            }
        };
    }
}
