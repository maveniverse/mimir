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

public interface LocalNodeFactory {
    /**
     * Creates a {@link LocalNode} instance.
     */
    LocalNode<?> createNode(Config config) throws IOException;
}
