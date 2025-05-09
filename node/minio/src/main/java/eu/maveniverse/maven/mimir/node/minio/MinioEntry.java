/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.node.minio;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.mimir.shared.impl.node.EntrySupport;
import eu.maveniverse.maven.mimir.shared.naming.Key;
import eu.maveniverse.maven.mimir.shared.node.SystemEntry;
import eu.maveniverse.maven.shared.core.fs.FileUtils;
import io.minio.DownloadObjectArgs;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.errors.MinioException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public final class MinioEntry extends EntrySupport implements SystemEntry {
    private final MinioClient minioClient;
    private final Key key;

    MinioEntry(Map<String, String> metadata, Map<String, String> checksums, MinioClient minioClient, Key key) {
        super(metadata, checksums);
        this.minioClient = requireNonNull(minioClient, "minioClient");
        this.key = requireNonNull(key, "key");
    }

    @Override
    public InputStream inputStream() throws IOException {
        try {
            return minioClient.getObject(GetObjectArgs.builder()
                    .bucket(key.container())
                    .object(key.name())
                    .build());
        } catch (MinioException e) {
            logger.debug(e.httpTrace());
            throw new IOException("inputStream()", e);
        } catch (Exception e) {
            throw new IOException("inputStream()", e);
        }
    }

    @Override
    public void transferTo(Path file) throws IOException {
        Files.deleteIfExists(file);
        try (FileUtils.CollocatedTempFile f = FileUtils.newTempFile(file)) {
            minioClient.downloadObject(DownloadObjectArgs.builder()
                    .bucket(key.container())
                    .object(key.name())
                    .filename(f.getPath().toString())
                    .build());
            f.move();
        } catch (MinioException e) {
            logger.debug(e.httpTrace());
            throw new IOException("transferTo()", e);
        } catch (Exception e) {
            throw new IOException("transferTo()", e);
        }
    }
}
