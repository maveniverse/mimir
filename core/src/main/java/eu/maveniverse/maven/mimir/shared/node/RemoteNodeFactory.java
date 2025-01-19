/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.shared.node;

import eu.maveniverse.maven.mimir.shared.Config;
import java.io.IOException;
import java.util.Optional;

public interface RemoteNodeFactory {
    /**
     * Creates a {@link RemoteNode} instance, or returns empty optional if for any reason cannot
     * contribute a node.
     */
    Optional<RemoteNode> createRemoteNode(Config config) throws IOException;
}
