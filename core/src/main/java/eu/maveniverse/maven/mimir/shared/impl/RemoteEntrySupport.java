/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.shared.impl;

import eu.maveniverse.maven.mimir.shared.node.Entry;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.util.Map;
import org.eclipse.aether.util.FileUtils;

public abstract class RemoteEntrySupport extends EntrySupport implements Entry {
    public RemoteEntrySupport(Map<String, String> metadata, Map<String, String> checksums) {
        super(metadata, checksums);
    }

    @Override
    public void transferTo(Path file) throws IOException {
        handleContent(inputStream -> {
            // TODO: ChecksumInputStream
            try (FileUtils.CollocatedTempFile f = FileUtils.newTempFile(file)) {
                Files.copy(inputStream, f.getPath(), StandardCopyOption.REPLACE_EXISTING);
                Files.setLastModifiedTime(
                        f.getPath(),
                        FileTime.fromMillis(Long.parseLong(metadata().getOrDefault(Entry.CONTENT_LAST_MODIFIED, "0"))));
                f.move();
            }
        });
    }

    @FunctionalInterface
    protected interface IOConsumer {
        void accept(InputStream stream) throws IOException;
    }

    protected abstract void handleContent(IOConsumer consumer) throws IOException;
}
