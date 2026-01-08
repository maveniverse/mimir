/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.shared.publisher;

import eu.maveniverse.maven.mimir.shared.SessionConfig;
import eu.maveniverse.maven.mimir.shared.node.LocalNode;
import java.io.IOException;

public interface PublisherFactory {
    /**
     * Creates a publisher for given system node.
     */
    Publisher createPublisher(SessionConfig sessionConfig, LocalNode localNode) throws IOException;
}
