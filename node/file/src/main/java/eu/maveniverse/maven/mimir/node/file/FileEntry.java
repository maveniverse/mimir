/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.node.file;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.mimir.shared.impl.Utils;
import eu.maveniverse.maven.mimir.shared.impl.node.EntrySupport;
import eu.maveniverse.maven.mimir.shared.node.SystemEntry;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.eclipse.aether.util.FileUtils;

public final class FileEntry extends EntrySupport implements SystemEntry {
    private final Path path;

    public FileEntry(Map<String, String> metadata, Map<String, String> checksums, Path path) {
        super(metadata, checksums);
        this.path = requireNonNull(path, "path");
    }

    @Override
    public InputStream inputStream() throws IOException {
        return Files.newInputStream(path);
    }

    @Override
    public void transferTo(Path file) throws IOException {
        Files.deleteIfExists(file);
        try (FileUtils.CollocatedTempFile f = FileUtils.newTempFile(file)) {
            Utils.copyOrLink(path, f.getPath());
            f.move();
        }
    }
}
