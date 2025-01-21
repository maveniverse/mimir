/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.shared.checksum;

import java.nio.ByteBuffer;

public interface ChecksumAlgorithm {
    /**
     * The factory created this instance.
     */
    ChecksumAlgorithmFactory factory();

    /**
     * Updates the checksum algorithm inner state with input.
     */
    void update(ByteBuffer input);

    /**
     * Updates the checksum algorithm inner state with input.
     */
    void update(byte[] input);

    /**
     * Returns the algorithm end result as string, never {@code null}. After invoking this method, this instance should
     * be discarded and not reused. For new checksum calculation you have to get new instance.
     */
    String checksum();
}
