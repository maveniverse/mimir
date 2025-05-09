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

/**
 * Local entry, can use file system as it is "local" to the caller.
 */
public interface LocalEntry extends Entry {
    /**
     * Transfers cached entry to given file by best available means and atomically. The file will be overwritten,
     * if existed. It is caller duty to figure out what should happen with existing target file (as this method will
     * delete it).
     */
    void transferTo(Path file) throws IOException;
}
