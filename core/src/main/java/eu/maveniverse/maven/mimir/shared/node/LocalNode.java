/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.shared.node;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

public interface LocalNode extends Node {
    /**
     * Stores cache entry and offers it as own entry.
     */
    LocalEntry store(Key key, Entry entry) throws IOException;

    /**
     * Stores file and offers it as own entry.
     */
    LocalEntry store(Key key, Path file, Map<String, String> checksums) throws IOException;
}
