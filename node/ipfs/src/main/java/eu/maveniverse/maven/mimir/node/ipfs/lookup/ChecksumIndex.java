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

/**
 * Checksum index that maintains entries. Aside of lookup, it can add entries as well.
 */
public interface ChecksumIndex extends ChecksumLookup {
    void add(URI key, String checksum) throws IOException;
}
