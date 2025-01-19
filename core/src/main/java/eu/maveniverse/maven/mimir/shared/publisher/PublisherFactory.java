/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.shared.publisher;

import eu.maveniverse.maven.mimir.shared.Config;
import eu.maveniverse.maven.mimir.shared.node.SystemEntry;
import java.io.IOException;
import java.util.Optional;
import java.util.function.Function;

public interface PublisherFactory {
    /**
     * Creates a publisher.
     */
    Publisher createPublisher(Config config, Function<String, Optional<SystemEntry>> entrySupplier) throws IOException;
}
