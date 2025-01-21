/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.node.minio;

import static eu.maveniverse.maven.mimir.shared.impl.Utils.mergeMetadataAndChecksums;
import static eu.maveniverse.maven.mimir.shared.impl.Utils.splitChecksums;
import static eu.maveniverse.maven.mimir.shared.impl.Utils.splitMetadata;
import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.mimir.shared.impl.node.NodeSupport;
import eu.maveniverse.maven.mimir.shared.naming.Key;
import eu.maveniverse.maven.mimir.shared.node.Entry;
import eu.maveniverse.maven.mimir.shared.node.LocalEntry;
import eu.maveniverse.maven.mimir.shared.node.RemoteEntry;
import eu.maveniverse.maven.mimir.shared.node.SystemEntry;
import eu.maveniverse.maven.mimir.shared.node.SystemNode;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.MinioException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import org.eclipse.aether.spi.connector.checksum.ChecksumAlgorithmFactory;

public final class MinioNode extends NodeSupport implements SystemNode {
    private final MinioNodeConfig config;
    private final MinioClient minioClient;
    private final Function<URI, Key> keyResolver;
    private final List<String> checksumAlgorithms;
    private final Map<String, ChecksumAlgorithmFactory> checksumFactories;

    public MinioNode(
            MinioNodeConfig config,
            MinioClient minioClient,
            Function<URI, Key> keyResolver,
            List<String> checksumAlgorithms,
            Map<String, ChecksumAlgorithmFactory> checksumFactories) {
        super(MinioNodeConfig.NAME);
        this.config = requireNonNull(config, "config");
        this.minioClient = requireNonNull(minioClient, "minioClient");
        this.keyResolver = requireNonNull(keyResolver, "keyResolver");
        this.checksumAlgorithms = requireNonNull(checksumAlgorithms, "checksumAlgorithms");
        this.checksumFactories = requireNonNull(checksumFactories, "checksumFactories");
    }

    @Override
    public List<String> checksumAlgorithms() {
        return checksumAlgorithms;
    }

    @Override
    public Map<String, ChecksumAlgorithmFactory> checksumFactories() {
        return checksumFactories;
    }

    @Override
    public Optional<MinioEntry> locate(URI key) throws IOException {
        ensureOpen();
        Key localKey = keyResolver.apply(key);
        try {
            StatObjectResponse stat = minioClient.statObject(StatObjectArgs.builder()
                    .bucket(localKey.container())
                    .object(localKey.name())
                    .build());
            Map<String, String> metadata = splitMetadata(stat.userMetadata());
            Map<String, String> checksums = splitChecksums(stat.userMetadata());
            return Optional.of(new MinioEntry(metadata, checksums, minioClient, localKey));
        } catch (ErrorResponseException e) {
            return Optional.empty();
        } catch (MinioException e) {
            logger.debug(e.httpTrace());
            throw new IOException("inputStream()", e);
        } catch (Exception e) {
            throw new IOException("inputStream()", e);
        }
    }

    @Override
    public MinioEntry store(URI key, Entry entry) throws IOException {
        ensureOpen();
        Key localKey = keyResolver.apply(key);
        long size = Long.parseLong(entry.metadata().get(Entry.CONTENT_LENGTH));
        switch (entry) {
            case RemoteEntry remoteEntry -> remoteEntry.handleContent(inputStream -> {
                try {
                    Map<String, String> userMetadata = mergeMetadataAndChecksums(entry.metadata(), entry.checksums());
                    minioClient.putObject(
                            PutObjectArgs.builder()
                                    .bucket(localKey.container())
                                    .object(localKey.name())
                                    .userMetadata(userMetadata)
                                    .stream(inputStream, size, -1)
                                    .build());
                } catch (MinioException e) {
                    logger.debug(e.httpTrace());
                    throw new IOException("inputStream()", e);
                } catch (Exception e) {
                    throw new IOException("inputStream()", e);
                }
            });
            case SystemEntry systemEntry -> {
                try (InputStream inputStream = systemEntry.inputStream()) {
                    Map<String, String> userMetadata = mergeMetadataAndChecksums(entry.metadata(), entry.checksums());
                    minioClient.putObject(
                            PutObjectArgs.builder()
                                    .bucket(localKey.container())
                                    .object(localKey.name())
                                    .userMetadata(userMetadata)
                                    .stream(inputStream, size, -1)
                                    .build());
                } catch (MinioException e) {
                    logger.debug(e.httpTrace());
                    throw new IOException("inputStream()", e);
                } catch (Exception e) {
                    throw new IOException("inputStream()", e);
                }
            }
            case LocalEntry localEntry -> {
                Path tempFile = Files.createTempFile(localKey.container(), "minio");
                localEntry.transferTo(tempFile);
                try (InputStream inputStream = Files.newInputStream(tempFile)) {
                    Map<String, String> userMetadata = mergeMetadataAndChecksums(entry.metadata(), entry.checksums());
                    minioClient.putObject(
                            PutObjectArgs.builder()
                                    .bucket(localKey.container())
                                    .object(localKey.name())
                                    .userMetadata(userMetadata)
                                    .stream(inputStream, size, -1)
                                    .build());
                } catch (MinioException e) {
                    logger.debug(e.httpTrace());
                    throw new IOException("inputStream()", e);
                } catch (Exception e) {
                    throw new IOException("inputStream()", e);
                }
            }
            default -> throw new UnsupportedOperationException("Unsupported entry type: " + entry.getClass());
        }
        return new MinioEntry(entry.metadata(), entry.checksums(), minioClient, localKey);
    }

    @Override
    public void store(URI key, Path file, Map<String, String> checksums) throws IOException {
        ensureOpen();
        Key localKey = keyResolver.apply(key);
        long size = Files.size(file);
        long lastModified = Files.getLastModifiedTime(file).toMillis();
        HashMap<String, String> metadata = new HashMap<>();
        metadata.put(Entry.CONTENT_LENGTH, Long.toString(size));
        metadata.put(Entry.CONTENT_LAST_MODIFIED, Long.toString(lastModified));
        try {
            Map<String, String> userMetadata = mergeMetadataAndChecksums(metadata, checksums);
            try (InputStream inputStream = Files.newInputStream(file)) {
                minioClient.putObject(
                        PutObjectArgs.builder()
                                .bucket(localKey.container())
                                .object(localKey.name())
                                .userMetadata(userMetadata)
                                .stream(inputStream, size, -1)
                                .build());
            }
        } catch (MinioException e) {
            logger.debug(e.httpTrace());
            throw new IOException("inputStream()", e);
        } catch (Exception e) {
            throw new IOException("inputStream()", e);
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + config.endpoint() + ")";
    }
}
