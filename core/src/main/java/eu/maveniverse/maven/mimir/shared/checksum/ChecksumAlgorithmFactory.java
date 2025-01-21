/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.shared.checksum;

public interface ChecksumAlgorithmFactory {
    /**
     * Returns the algorithm name, usually used as key, never {@code null} value. The name is a standard name of
     * algorithm (if applicable) or any other designator that is algorithm commonly referred with. Example: "SHA-1".
     */
    String getName();

    /**
     * Returns the file extension to be used for given checksum file (without leading dot), never {@code null}. The
     * extension should be file and URL path friendly, and may differ from value returned by {@link #getName()}.
     * The checksum extension SHOULD NOT contain dot (".") character.
     * Example: "sha1".
     */
    String getFileExtension();

    /**
     * Each invocation of this method returns a new instance of algorithm, never {@code null} value.
     */
    ChecksumAlgorithm getAlgorithm();
}
