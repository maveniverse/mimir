/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.node.file;

import static eu.maveniverse.maven.mimir.shared.impl.Utils.mergeEntry;
import static eu.maveniverse.maven.mimir.shared.impl.Utils.splitChecksums;
import static eu.maveniverse.maven.mimir.shared.impl.Utils.splitMetadata;
import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.mimir.shared.impl.FileUtils;
import eu.maveniverse.maven.mimir.shared.impl.checksum.ChecksumEnforcer;
import eu.maveniverse.maven.mimir.shared.impl.checksum.ChecksumInputStream;
import eu.maveniverse.maven.mimir.shared.impl.node.NodeSupport;
import eu.maveniverse.maven.mimir.shared.naming.Key;
import eu.maveniverse.maven.mimir.shared.node.Entry;
import eu.maveniverse.maven.mimir.shared.node.LocalEntry;
import eu.maveniverse.maven.mimir.shared.node.RemoteEntry;
import eu.maveniverse.maven.mimir.shared.node.SystemNode;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileTime;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.eclipse.aether.spi.connector.checksum.ChecksumAlgorithm;
import org.eclipse.aether.spi.connector.checksum.ChecksumAlgorithmFactory;
import org.msgpack.core.MessagePack;
import org.msgpack.core.MessagePacker;
import org.msgpack.core.MessageUnpacker;

public final class FileNode extends NodeSupport implements SystemNode {
    private final Path basedir;
    private final boolean mayLink;
    private final Function<URI, Key> keyResolver;
    private final List<String> checksumAlgorithms;
    private final Map<String, ChecksumAlgorithmFactory> checksumFactories;

    public FileNode(
            Path basedir,
            boolean mayLink,
            Function<URI, Key> keyResolver,
            List<String> checksumAlgorithms,
            Map<String, ChecksumAlgorithmFactory> checksumFactories)
            throws IOException {
        super(FileNodeConfig.NAME);
        this.basedir = basedir;
        this.mayLink = mayLink;
        this.keyResolver = requireNonNull(keyResolver, "keyResolver");
        this.checksumAlgorithms = List.copyOf(checksumAlgorithms);
        this.checksumFactories = Map.copyOf(checksumFactories);

        Files.createDirectories(basedir);
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
    public Optional<FileEntry> locate(URI key) throws IOException {
        ensureOpen();
        Path path = resolveKey(key);
        if (Files.isRegularFile(path)) {
            Map<String, String> data = loadMetadata(path);
            return Optional.of(createEntry(path, splitMetadata(data), splitChecksums(data)));
        } else {
            return Optional.empty();
        }
    }

    @Override
    public FileEntry store(URI key, Entry entry) throws IOException {
        ensureOpen();
        Path path = resolveKey(key);
        if (entry instanceof RemoteEntry remoteEntry) {
            try (FileUtils.CollocatedTempFile f = FileUtils.newTempFile(path)) {
                remoteEntry.handleContent(inputStream -> {
                    ChecksumEnforcer checksumEnforcer;
                    try (InputStream enforced = new ChecksumInputStream(
                            inputStream,
                            checksumAlgorithms().stream()
                                    .map(a -> new AbstractMap.SimpleEntry<>(
                                            a, checksumFactories.get(a).getAlgorithm()))
                                    .collect(Collectors.toMap(
                                            AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue)),
                            checksumEnforcer = new ChecksumEnforcer(entry.checksums()))) {
                        Files.copy(enforced, f.getPath(), StandardCopyOption.REPLACE_EXISTING);
                    }
                    storeMetadata(path, mergeEntry(entry.metadata(), checksumEnforcer.getChecksums()));
                    f.move();
                });
            }
        } else if (entry instanceof LocalEntry localEntry) {
            storeMetadata(path, mergeEntry(entry));
            localEntry.transferTo(path);
        } else {
            throw new UnsupportedOperationException("Unsupported entry type: " + entry.getClass());
        }
        return createEntry(path, entry.metadata(), entry.checksums());
    }

    @Override
    public FileEntry store(URI key, Path file, Map<String, String> md, Map<String, String> checksums)
            throws IOException {
        ensureOpen();
        Path path = resolveKey(key);
        HashMap<String, String> metadata = new HashMap<>(md);
        FileTime fileTime = Files.getLastModifiedTime(file);
        ChecksumEnforcer checksumEnforcer;
        try (FileUtils.CollocatedTempFile f = FileUtils.newTempFile(path)) {
            try (InputStream enforced = new ChecksumInputStream(
                    Files.newInputStream(file),
                    checksumAlgorithms().stream()
                            .map(a -> new AbstractMap.SimpleEntry<>(
                                    a, checksumFactories.get(a).getAlgorithm()))
                            .collect(Collectors.toMap(
                                    AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue)),
                    checksumEnforcer = new ChecksumEnforcer(checksums))) {
                Files.copy(enforced, f.getPath());
                Files.setLastModifiedTime(f.getPath(), fileTime);
            }

            Entry.setContentLength(metadata, Files.size(file));
            Entry.setContentLastModified(metadata, fileTime.toInstant());
            storeMetadata(path, mergeEntry(metadata, checksumEnforcer.getChecksums()));
            f.move();
        }
        return new FileEntry(metadata, checksumEnforcer.getChecksums(), path, mayLink);
    }

    private Path resolveKey(URI key) {
        Key resolved = keyResolver.apply(key);
        return basedir.resolve(resolved.container()).resolve(resolved.name());
    }

    private FileEntry createEntry(Path file, Map<String, String> metadata, Map<String, String> checksums)
            throws IOException {
        HashMap<String, String> md = new HashMap<>(metadata);
        Entry.setContentLength(md, Files.size(file));
        Entry.setContentLastModified(md, Files.getLastModifiedTime(file).toInstant());
        return new FileEntry(md, checksums, file, mayLink);
    }

    private void storeMetadata(Path file, Map<String, String> metadata) throws IOException {
        Path md = file.getParent().resolve("_" + file.getFileName());
        try (MessagePacker packer = MessagePack.newDefaultPacker(
                Files.newOutputStream(md, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING))) {
            packer.packMapHeader(metadata.size());
            for (Map.Entry<String, String> entry : metadata.entrySet()) {
                packer.packString(entry.getKey());
                packer.packString(entry.getValue());
            }
        }
    }

    private Map<String, String> loadMetadata(Path file) throws IOException {
        Path md = file.getParent().resolve("_" + file.getFileName());
        if (!Files.isRegularFile(md)) {
            recreateMetadata(file);
        }
        try (MessageUnpacker unpacker = MessagePack.newDefaultUnpacker(Files.newInputStream(md))) {
            HashMap<String, String> metadata = new HashMap<>();
            int entries = unpacker.unpackMapHeader();
            for (int i = 0; i < entries; i++) {
                String key = unpacker.unpackString();
                String value = unpacker.unpackString();
                metadata.put(key, value);
            }
            return metadata;
        }
    }

    private void recreateMetadata(Path file) throws IOException {
        HashMap<String, String> metadata = new HashMap<>();
        Entry.setContentLength(metadata, Files.size(file));
        Entry.setContentLastModified(metadata, Files.getLastModifiedTime(file).toInstant());
        Map<String, String> checksums = calculate(
                file,
                checksumAlgorithms().stream()
                        .map(a -> new AbstractMap.SimpleEntry<>(
                                a, checksumFactories.get(a).getAlgorithm()))
                        .collect(Collectors.toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue)));
        storeMetadata(file, mergeEntry(metadata, checksums));
    }

    private static Map<String, String> calculate(Path path, Map<String, ChecksumAlgorithm> checksums)
            throws IOException {
        AtomicReference<Map<String, String>> checksumsRef = new AtomicReference<>(null);
        try (InputStream stream = new ChecksumInputStream(Files.newInputStream(path), checksums, checksumsRef::set)) {
            stream.transferTo(OutputStream.nullOutputStream());
        }
        return checksumsRef.get();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " (basedir=" + basedir + " mayLink=" + mayLink + ")";
    }
}
