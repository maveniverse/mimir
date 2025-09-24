/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.node.bundle;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.mimir.shared.impl.node.EntrySupport;
import eu.maveniverse.maven.mimir.shared.node.LocalEntry;
import eu.maveniverse.maven.shared.core.fs.FileUtils;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public final class BundleEntry extends EntrySupport implements LocalEntry {
    private final Path bundleFsPath;

    public BundleEntry(Map<String, String> metadata, Map<String, String> checksums, Path bundleFsPath) {
        super(metadata, checksums);
        this.bundleFsPath = requireNonNull(bundleFsPath, "bundleFsPath");
    }

    @Override
    public void transferTo(Path file) throws IOException {
        Files.deleteIfExists(file);
        try (FileUtils.CollocatedTempFile f = FileUtils.newTempFile(file)) {
            FileUtils.copy(bundleFsPath, f.getPath());
            f.move();
        }
    }
}
