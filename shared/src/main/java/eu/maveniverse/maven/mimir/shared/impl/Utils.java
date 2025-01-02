/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.shared.impl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public final class Utils {
    private Utils() {}

    /**
     * Performs a hard-linking (if on same volume), otherwise plain copies file contents. Does not check for
     * any precondition (source exists and is regular file, destination does not exist), it is caller job.
     */
    public static void copyOrLink(Path src, Path dest) throws IOException {
        Files.createDirectories(dest.getParent());
        if (Objects.equals(Files.getFileStore(src), Files.getFileStore(dest.getParent()))) {
            Files.createLink(dest, src);
        } else {
            Files.copy(src, dest);
        }
        Files.setLastModifiedTime(dest, Files.getLastModifiedTime(src));
    }
}
