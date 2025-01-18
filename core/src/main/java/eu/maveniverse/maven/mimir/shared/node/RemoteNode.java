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

public interface RemoteNode extends Node {
    /**
     * Locates cache entry by key on this node.
     */
    @Override
    Optional<RemoteEntry> locate(URI key) throws IOException;
}
