/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.node.ipfs.lookup;

import java.io.IOException;
import java.net.URI;
import java.util.Optional;

/**
 * Checksum lookup. Lookup can happen from:
 * <ul>
 *     <li>local index</li>
 *     <li>remote repository</li>
 * </ul>
 */
public interface ChecksumLookup {
    /**
     * Looks up asked checksums corresponding to given repository, artifact and algorithms.
     */
    Optional<String> lookup(URI key) throws IOException;
}
