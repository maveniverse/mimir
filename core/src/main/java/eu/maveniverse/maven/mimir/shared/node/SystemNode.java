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
import java.util.Map;
import java.util.Optional;

/**
 * Local node uses some "local" (local to the system) backing storage.
 * This type of node is able to back a session.
 */
public interface SystemNode extends LocalNode {
    /**
     * Locates cache entry by key on this node.
     */
    @Override
    Optional<? extends SystemEntry> locate(URI key) throws IOException;

    /**
     * Stores file as new entry.
     */
    @Override
    SystemEntry store(URI key, Path file, Map<String, String> metadata, Map<String, String> checksums)
            throws IOException;

    /**
     * Stores entry and provides new local entry for stored content.
     */
    SystemEntry store(URI key, Entry entry) throws IOException;
}
