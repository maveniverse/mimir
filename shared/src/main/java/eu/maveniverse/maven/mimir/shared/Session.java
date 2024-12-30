/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.shared;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

public interface Session extends AutoCloseable {
    Optional<CacheEntry> locate(CacheKey key) throws IOException;

    boolean store(CacheKey key, Path content) throws IOException;
}
