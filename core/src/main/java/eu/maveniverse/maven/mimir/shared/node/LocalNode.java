/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.shared.node;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.eclipse.aether.spi.connector.checksum.ChecksumAlgorithmFactory;

/**
 * Local node uses some "local" (local to the system) backing storage and hence, has access to filesystem.
 * This type of node is able to back a session.
 */
public interface LocalNode<E extends LocalEntry> extends Node<E> {
    /**
     * Provides list of checksum algorithm names configured to be used by this node.
     */
    List<String> checksumAlgorithms() throws IOException;

    /**
     * Provides all supported checksum factories keyed by algorithm name.
     */
    Map<String, ChecksumAlgorithmFactory> checksumFactories() throws IOException;

    /**
     * Stores file as new entry.
     */
    E store(URI key, Path file, Map<String, String> metadata, Map<String, String> checksums) throws IOException;
}
