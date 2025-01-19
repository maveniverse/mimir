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
import java.util.Optional;

/**
 * Local node uses some "local" (local to the system) backing storage.
 * This type of node is able to back a Mimir session.
 */
public interface SystemNode extends LocalNode {
    /**
     * Locates cache entry by key on this node.
     */
    @Override
    Optional<? extends SystemEntry> locate(URI key) throws IOException;

    /**
     * Stores cache entry and offers it as own entry.
     */
    SystemEntry store(URI key, Entry entry) throws IOException;
}
