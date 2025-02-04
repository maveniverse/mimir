/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.node.daemon;

import static eu.maveniverse.maven.mimir.node.daemon.DaemonProtocol.readHelloRsp;
import static eu.maveniverse.maven.mimir.node.daemon.DaemonProtocol.readLocateRsp;
import static eu.maveniverse.maven.mimir.node.daemon.DaemonProtocol.readLsChecksumsRsp;
import static eu.maveniverse.maven.mimir.node.daemon.DaemonProtocol.readStorePathRsp;
import static eu.maveniverse.maven.mimir.node.daemon.DaemonProtocol.readTransferRsp;
import static eu.maveniverse.maven.mimir.node.daemon.DaemonProtocol.writeHelloReq;
import static eu.maveniverse.maven.mimir.node.daemon.DaemonProtocol.writeLocateReq;
import static eu.maveniverse.maven.mimir.node.daemon.DaemonProtocol.writeLsChecksumsReq;
import static eu.maveniverse.maven.mimir.node.daemon.DaemonProtocol.writeStorePathReq;
import static eu.maveniverse.maven.mimir.node.daemon.DaemonProtocol.writeTransferReq;
import static eu.maveniverse.maven.mimir.shared.impl.Utils.splitChecksums;
import static eu.maveniverse.maven.mimir.shared.impl.Utils.splitMetadata;
import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.mimir.shared.Config;
import eu.maveniverse.maven.mimir.shared.impl.node.EntrySupport;
import eu.maveniverse.maven.mimir.shared.impl.node.NodeSupport;
import eu.maveniverse.maven.mimir.shared.node.LocalEntry;
import eu.maveniverse.maven.mimir.shared.node.LocalNode;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.UnixDomainSocketAddress;
import java.nio.channels.Channels;
import java.nio.channels.SocketChannel;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.eclipse.aether.spi.connector.checksum.ChecksumAlgorithmFactory;

/**
 * This node is delegating all the work to daemon via Unix Domain Sockets.
 */
public class DaemonNode extends NodeSupport implements LocalNode {
    private final Path socketPath;
    private final Map<String, ChecksumAlgorithmFactory> checksumFactories;

    public DaemonNode(
            Map<String, String> clientData, Path socketPath, Map<String, ChecksumAlgorithmFactory> checksumFactories)
            throws IOException {
        super(DaemonConfig.NAME);
        this.socketPath = requireNonNull(socketPath, "socketPath");
        this.checksumFactories = Collections.unmodifiableMap(requireNonNull(checksumFactories, "checksumFactories"));

        try (Handle handle = create()) {
            writeHelloReq(handle.dos, clientData);
            Map<String, String> data = readHelloRsp(handle.dis);
            logger.debug("Hello OK {}", data);
        }
    }

    @Override
    public List<String> checksumAlgorithms() throws IOException {
        try (Handle handle = create()) {
            writeLsChecksumsReq(handle.dos);
            return readLsChecksumsRsp(handle.dis);
        }
    }

    @Override
    public Map<String, ChecksumAlgorithmFactory> checksumFactories() {
        return checksumFactories;
    }

    @Override
    public Optional<LocalEntry> locate(URI key) throws IOException {
        String keyString = key.toASCIIString();
        logger.debug("LOCATE '{}'", keyString);
        try (Handle handle = create()) {
            writeLocateReq(handle.dos, keyString);
            Map<String, String> response = readLocateRsp(handle.dis);
            if (!response.isEmpty()) {
                return Optional.of(new DaemonEntry(splitMetadata(response), splitChecksums(response), keyString));
            } else {
                return Optional.empty();
            }
        }
    }

    @Override
    public void store(URI key, Path file, Map<String, String> checksums) throws IOException {
        String keyString = key.toASCIIString();
        String filePath = Config.getCanonicalPath(file).toString();
        logger.debug("STORE PATH '{}' -> '{}'", keyString, filePath);
        try (Handle handle = create()) {
            writeStorePathReq(handle.dos, keyString, filePath, checksums);
            readStorePathRsp(handle.dis);
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " (socketPath=" + socketPath + ")";
    }

    private Handle create() throws IOException {
        return new Handle();
    }

    private class Handle implements Closeable {
        private final SocketChannel socketChannel;
        private final DataOutputStream dos;
        private final DataInputStream dis;

        public Handle() throws IOException {
            this.socketChannel = SocketChannel.open(UnixDomainSocketAddress.of(socketPath));
            this.dos = new DataOutputStream(new BufferedOutputStream(Channels.newOutputStream(socketChannel)));
            this.dis = new DataInputStream(new BufferedInputStream(Channels.newInputStream(socketChannel)));
        }

        @Override
        public void close() throws IOException {
            socketChannel.close();
        }
    }

    private class DaemonEntry extends EntrySupport implements LocalEntry {
        private final String keyString;

        private DaemonEntry(Map<String, String> metadata, Map<String, String> checksums, String keyString) {
            super(metadata, checksums);
            this.keyString = keyString;
        }

        @Override
        public void transferTo(Path file) throws IOException {
            logger.debug("TRANSFER '{}'->'{}'", keyString, file);
            try (Handle handle = create()) {
                writeTransferReq(
                        handle.dos, keyString, Config.getCanonicalPath(file).toString());
                readTransferRsp(handle.dis);
            }
        }
    }
}
