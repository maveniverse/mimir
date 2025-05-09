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

/**
 * System node is a special local node that can also cache various {@link Entry} items, and provides system entry,
 * that makes publishing cache possible as well. On one workstation there is usually one system node, that is also
 * published.
 */
public interface SystemNode<E extends SystemEntry> extends LocalNode<E> {
    /**
     * Returns {@code true} if it is guaranteed that this system node has exclusive access to storage.
     */
    default boolean exclusiveAccess() {
        return false;
    }

    /**
     * Returns {@code true} if caches were purged. Implementation may choose to not purge caches, among other
     * things due lack of {@link #exclusiveAccess()}.
     */
    default boolean purgeCaches() {
        return false;
    }

    /**
     * Stores entry and provides new local entry for stored content. If entry already exists, method will fail if
     * checksums are not matching (not "same file").
     */
    E store(URI key, Entry entry) throws IOException;
}
