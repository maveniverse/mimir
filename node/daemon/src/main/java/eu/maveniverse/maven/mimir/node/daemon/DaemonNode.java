/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.node.daemon;

import static eu.maveniverse.maven.mimir.shared.impl.Utils.mergeEntry;
import static eu.maveniverse.maven.mimir.shared.impl.Utils.splitChecksums;
import static eu.maveniverse.maven.mimir.shared.impl.Utils.splitMetadata;
import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.mimir.node.daemon.protocol.Handle;
import eu.maveniverse.maven.mimir.node.daemon.protocol.Request;
import eu.maveniverse.maven.mimir.node.daemon.protocol.Response;
import eu.maveniverse.maven.mimir.shared.Config;
import eu.maveniverse.maven.mimir.shared.impl.node.EntrySupport;
import eu.maveniverse.maven.mimir.shared.impl.node.NodeSupport;
import eu.maveniverse.maven.mimir.shared.node.LocalEntry;
import eu.maveniverse.maven.mimir.shared.node.LocalNode;
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
public class DaemonNode extends NodeSupport<DaemonNode.DaemonEntry> implements LocalNode<DaemonNode.DaemonEntry> {
    private final Path socketPath;
    private final Map<String, ChecksumAlgorithmFactory> checksumFactories;
    private final boolean autostop;
    private final Map<String, String> session;

    public DaemonNode(
            Map<String, String> clientData,
            Path socketPath,
            Map<String, ChecksumAlgorithmFactory> checksumFactories,
            boolean autostop)
            throws IOException {
        super(DaemonConfig.NAME);
        this.socketPath = requireNonNull(socketPath, "socketPath");
        this.checksumFactories = Collections.unmodifiableMap(requireNonNull(checksumFactories, "checksumFactories"));
        this.autostop = autostop;

        try (Handle handle = create()) {
            handle.writeRequest(Request.hello(clientData));
            Response helloResponse = handle.readResponse();
            this.session = helloResponse.session();
            logger.debug("Hello OK {}", helloResponse.data());
        }
    }

    @Override
    public List<String> checksumAlgorithms() throws IOException {
        try (Handle handle = create()) {
            handle.writeRequest(Request.lsChecksums(session));
            return Handle.mapToList(handle.readResponse().data());
        }
    }

    @Override
    public Map<String, ChecksumAlgorithmFactory> checksumFactories() {
        return checksumFactories;
    }

    @Override
    public Optional<DaemonEntry> locate(URI key) throws IOException {
        String keyString = key.toASCIIString();
        logger.debug("LOCATE '{}'", keyString);
        try (Handle handle = create()) {
            handle.writeRequest(Request.locate(session, keyString));
            Response locateResponse = handle.readResponse();
            if (!locateResponse.data().isEmpty()) {
                return Optional.of(new DaemonEntry(
                        splitMetadata(locateResponse.data()), splitChecksums(locateResponse.data()), keyString));
            } else {
                return Optional.empty();
            }
        }
    }

    @Override
    public DaemonEntry store(URI key, Path file, Map<String, String> metadata, Map<String, String> checksums)
            throws IOException {
        String keyString = key.toASCIIString();
        String filePath = Config.getCanonicalPath(file).toString();
        logger.debug("STORE PATH '{}' -> '{}'", keyString, filePath);
        try (Handle handle = create()) {
            handle.writeRequest(Request.storePath(session, keyString, filePath, mergeEntry(metadata, checksums)));
            Response storePathResponse = handle.readResponse();
            if (!storePathResponse.data().isEmpty()) {
                return new DaemonEntry(
                        splitMetadata(storePathResponse.data()), splitChecksums(storePathResponse.data()), keyString);
            } else {
                // this theoretically can never happen: daemon will either store or fail
                throw new IOException("Failed to store " + filePath + "; check daemon logs");
            }
        }
    }

    @Override
    protected void doClose() throws IOException {
        try (Handle handle = create()) {
            if (autostop) {
                logger.info("Daemon shutdown initiated");
            }
            handle.writeRequest(Request.bye(session, autostop));
            Response byeResponse = handle.readResponse();
            logger.debug("Bye OK {}", byeResponse.data());
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " (socketPath=" + socketPath + "; daemonPID="
                + session.getOrDefault("daemon.pid", "n/a") + "; daemonVersion="
                + session.getOrDefault("daemon.version", "n/a") + ")";
    }

    private Handle create() throws IOException {
        SocketChannel socketChannel = SocketChannel.open(UnixDomainSocketAddress.of(socketPath));
        socketChannel.configureBlocking(true);
        return new Handle(Channels.newOutputStream(socketChannel), Channels.newInputStream(socketChannel)) {
            @Override
            public void close() throws IOException {
                try (socketChannel) {
                    super.close();
                }
            }
        };
    }

    public class DaemonEntry extends EntrySupport implements LocalEntry {
        private final String keyString;

        private DaemonEntry(Map<String, String> metadata, Map<String, String> checksums, String keyString) {
            super(metadata, checksums);
            this.keyString = keyString;
        }

        @Override
        public void transferTo(Path file) throws IOException {
            logger.debug("TRANSFER '{}'->'{}'", keyString, file);
            try (Handle handle = create()) {
                handle.writeRequest(Request.transferTo(
                        session, keyString, Config.getCanonicalPath(file).toString()));
                handle.readResponse();
            }
        }
    }
}
