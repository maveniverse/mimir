/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.node.file;

import static eu.maveniverse.maven.mimir.shared.impl.EntryUtils.mergeEntry;
import static eu.maveniverse.maven.mimir.shared.impl.EntryUtils.splitChecksums;
import static eu.maveniverse.maven.mimir.shared.impl.EntryUtils.splitMetadata;
import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.mimir.shared.impl.checksum.ChecksumEnforcer;
import eu.maveniverse.maven.mimir.shared.impl.checksum.ChecksumInputStream;
import eu.maveniverse.maven.mimir.shared.impl.node.NodeSupport;
import eu.maveniverse.maven.mimir.shared.naming.Key;
import eu.maveniverse.maven.mimir.shared.node.Entry;
import eu.maveniverse.maven.mimir.shared.node.LocalEntry;
import eu.maveniverse.maven.mimir.shared.node.RemoteEntry;
import eu.maveniverse.maven.mimir.shared.node.SystemNode;
import eu.maveniverse.maven.shared.core.fs.DirectoryLocker;
import eu.maveniverse.maven.shared.core.fs.FileUtils;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
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

public final class FileNode extends NodeSupport implements SystemNode {

    private final Path basedir;
    private final Path baseLockDir;
    private final boolean mayLink;
    private final boolean exclusiveAccess;
    private final FileNodeConfig.CachePurge cachePurge;
    private final Function<URI, Key> keyResolver;
    private final List<String> checksumAlgorithms;
    private final Map<String, ChecksumAlgorithmFactory> checksumFactories;
    private final DirectoryLocker directoryLocker;

    private final Path shadowBasedir;

    public FileNode(
            Path basedir,
            Path baseLockDir,
            boolean mayLink,
            boolean exclusiveAccess,
            FileNodeConfig.CachePurge cachePurge,
            Function<URI, Key> keyResolver,
            List<String> checksumAlgorithms,
            Map<String, ChecksumAlgorithmFactory> checksumFactories,
            DirectoryLocker directoryLocker)
            throws IOException {
        super(FileNodeConfig.NAME);
        this.mayLink = mayLink;
        this.exclusiveAccess = exclusiveAccess;
        this.cachePurge = cachePurge;
        this.keyResolver = requireNonNull(keyResolver, "keyResolver");
        this.checksumAlgorithms = List.copyOf(checksumAlgorithms);
        this.checksumFactories = Map.copyOf(checksumFactories);
        this.directoryLocker = requireNonNull(directoryLocker);

        if (cachePurge != FileNodeConfig.CachePurge.OFF && !exclusiveAccess) {
            throw new IllegalArgumentException(
                    "Invalid configuration: cachePurge possible only with exclusiveAccess enabled");
        }

        Files.createDirectories(basedir);
        Files.createDirectories(baseLockDir);
        this.baseLockDir = baseLockDir;
        this.directoryLocker.lockDirectory(baseLockDir, exclusiveAccess);

        // at this point, if cachePurge != OFF we have exclusiveAccess=true and we "own" exclusive lock over storage
        if (cachePurge == FileNodeConfig.CachePurge.OFF) {
            // normal operation
            this.basedir = basedir;
            this.shadowBasedir = null;
        } else if (cachePurge == FileNodeConfig.CachePurge.ON_BEGIN) {
            // in this mode we move (if exists) our basedir to shadow basedir, and whatever is touched we pull back
            this.basedir = basedir;
            this.shadowBasedir = basedir.getParent().resolve(basedir.getFileName() + "-" + System.nanoTime());
            if (Files.isDirectory(this.basedir)) {
                if (Files.isDirectory(this.shadowBasedir)) {
                    FileUtils.deleteRecursively(this.shadowBasedir);
                }
                Files.move(
                        this.basedir,
                        this.shadowBasedir,
                        StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            }
            Files.createDirectories(this.basedir);
            Files.createDirectories(this.shadowBasedir);
        } else if (cachePurge == FileNodeConfig.CachePurge.ON_END) {
            // in this mode we leave (if exists) our basedir in place, and will copy touched elements to shadow basedir
            // and swap them at end
            this.basedir = basedir.getParent().resolve(basedir.getFileName() + "-" + System.nanoTime());
            this.shadowBasedir = basedir;
            Files.createDirectories(this.basedir);
            Files.createDirectories(this.shadowBasedir);
        } else {
            throw new IllegalArgumentException("Unsupported CachePurge mode: " + cachePurge);
        }
    }

    @Override
    public List<String> checksumAlgorithms() {
        return checksumAlgorithms;
    }

    @Override
    public Optional<FileEntry> locate(URI key) throws IOException {
        checkClosed();
        Path path = resolveKey(key, true);
        if (Files.isRegularFile(path)) {
            Map<String, String> data = loadMetadata(path);
            return Optional.of(createEntry(path, splitMetadata(data), splitChecksums(data)));
        } else {
            return Optional.empty();
        }
    }

    @Override
    public FileEntry store(URI key, Path file, Map<String, String> md, Map<String, String> checksums)
            throws IOException {
        checkClosed();
        Path path = resolveKey(key, false);
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

    @Override
    public boolean exclusiveAccess() {
        return exclusiveAccess;
    }

    @Override
    public FileEntry store(URI key, Entry entry) throws IOException {
        checkClosed();
        Path path = resolveKey(key, false);
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

    private Path resolveKey(URI key, boolean mayHandleCachePurge) throws IOException {
        Key resolved = this.keyResolver.apply(key);
        Path target = this.basedir.resolve(resolved.container()).resolve(resolved.name());
        if (mayHandleCachePurge && cachePurge != FileNodeConfig.CachePurge.OFF && !Files.isRegularFile(target)) {
            Path shadow = this.shadowBasedir.resolve(resolved.container()).resolve(resolved.name());
            try {
                if (Files.isRegularFile(shadow)) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Found shadow for key: {}@{}", resolved.container(), resolved.name());
                    }
                    Path targetMd = target.getParent().resolve("_" + target.getFileName());
                    Path shadowMd = shadow.getParent().resolve("_" + shadow.getFileName());
                    Files.createDirectories(target.getParent());
                    switch (cachePurge) {
                        case ON_BEGIN -> {
                            Files.move(
                                    shadowMd,
                                    targetMd,
                                    StandardCopyOption.ATOMIC_MOVE,
                                    StandardCopyOption.REPLACE_EXISTING);
                            Files.move(
                                    shadow,
                                    target,
                                    StandardCopyOption.ATOMIC_MOVE,
                                    StandardCopyOption.REPLACE_EXISTING);
                        }
                        case ON_END -> {
                            Files.copy(
                                    shadowMd,
                                    targetMd,
                                    StandardCopyOption.REPLACE_EXISTING,
                                    StandardCopyOption.COPY_ATTRIBUTES);
                            Files.copy(
                                    shadow,
                                    target,
                                    StandardCopyOption.REPLACE_EXISTING,
                                    StandardCopyOption.COPY_ATTRIBUTES);
                        }
                        default -> throw new IllegalArgumentException("Unsupported CachePurge mode: " + cachePurge);
                    }
                }
            } catch (IOException e) {
                logger.warn("Unable apply cache-purge to path '{}'", target, e);
                try {
                    FileUtils.deleteRecursively(shadow.getParent());
                } catch (IOException e1) {
                    logger.warn("Unable to drop shadow '{}'", shadow, e);
                }
            }
        }
        return target;
    }

    private FileEntry createEntry(Path file, Map<String, String> metadata, Map<String, String> checksums)
            throws IOException {
        HashMap<String, String> md = new HashMap<>(metadata);
        Entry.setContentLength(md, Files.size(file));
        Entry.setContentLastModified(md, Files.getLastModifiedTime(file).toInstant());
        return new FileEntry(md, checksums, file, mayLink);
    }

    @Override
    protected void doClose() throws IOException {
        try {
            if (cachePurge == FileNodeConfig.CachePurge.ON_BEGIN) {
                // just delete shadow; we pulled all we needed
                logger.info("Purge on begin; cleanup");
                FileUtils.deleteRecursively(this.shadowBasedir);
            } else if (cachePurge == FileNodeConfig.CachePurge.ON_END) {
                // swap out shadow and basedir
                logger.info("Purge on end; performing swap-out of storage");
                Path backup =
                        this.basedir.getParent().resolve(this.basedir.getFileName() + "-purge-" + System.nanoTime());
                Files.move(this.basedir, backup, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                Files.move(
                        this.shadowBasedir,
                        this.basedir,
                        StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
                FileUtils.deleteRecursively(backup);
            }
        } finally {
            directoryLocker.unlockDirectory(baseLockDir);
        }
    }

    private void storeMetadata(Path file, Map<String, String> metadata) throws IOException {
        Path md = file.getParent().resolve("_" + file.getFileName());
        try (ObjectOutputStream oos = new ObjectOutputStream(
                Files.newOutputStream(md, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING))) {
            oos.writeInt(metadata.size());
            for (Map.Entry<String, String> entry : metadata.entrySet()) {
                oos.writeUTF(entry.getKey());
                oos.writeUTF(entry.getValue());
            }
        }
    }

    private Map<String, String> loadMetadata(Path file) throws IOException {
        Path md = file.getParent().resolve("_" + file.getFileName());
        if (!Files.isRegularFile(md)) {
            recreateMetadata(file);
        }
        try (ObjectInputStream ois = new ObjectInputStream(Files.newInputStream(md))) {
            HashMap<String, String> metadata = new HashMap<>();
            int entries = ois.readInt();
            for (int i = 0; i < entries; i++) {
                String key = ois.readUTF();
                String value = ois.readUTF();
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
        return getClass().getSimpleName() + " (basedir=" + basedir + " mayLink=" + mayLink + " exclusiveAccess="
                + exclusiveAccess + " cachePurge=" + cachePurge + ")";
    }
}
