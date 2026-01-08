/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.shared.publisher;

import eu.maveniverse.maven.mimir.shared.node.LocalEntry;
import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.util.Optional;

public interface Publisher extends Closeable {
    /**
     * The publisher handle.
     */
    interface Handle {
        /**
         * The handle.
         */
        URI handle();

        /**
         * The entry being published under given handle.
         */
        LocalEntry publishedEntry();
    }

    /**
     * Crafts a publisher specific URI based on passed in content URI key, if content can be published.
     */
    Optional<Handle> createHandle(URI key) throws IOException;
}
