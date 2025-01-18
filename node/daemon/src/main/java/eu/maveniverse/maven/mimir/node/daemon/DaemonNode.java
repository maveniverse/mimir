/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.node.daemon;

import static eu.maveniverse.maven.mimir.shared.impl.SimpleProtocol.readLocateRsp;
import static eu.maveniverse.maven.mimir.shared.impl.SimpleProtocol.readTransferRsp;
import static eu.maveniverse.maven.mimir.shared.impl.SimpleProtocol.writeLocateReq;
import static eu.maveniverse.maven.mimir.shared.impl.SimpleProtocol.writeTransferReq;
import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.mimir.shared.impl.EntrySupport;
import eu.maveniverse.maven.mimir.shared.impl.NodeSupport;
import eu.maveniverse.maven.mimir.shared.node.Entry;
import eu.maveniverse.maven.mimir.shared.node.RemoteNode;
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
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This node is delegating all the work to daemon via Unix Domain Sockets.
 */
public class DaemonNode extends NodeSupport implements RemoteNode {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final Path socketPath;

    public DaemonNode(Path socketPath) {
        super(DaemonConfig.NAME, 10);
        this.socketPath = requireNonNull(socketPath, "socketPath");
    }

    @Override
    public Optional<Entry> locate(URI key) throws IOException {
        String keyString = key.toASCIIString();
        logger.debug("LOCATE '{}'", keyString);
        try (Handle handle = create()) {
            writeLocateReq(handle.dos, keyString);
            Map<String, String> response = readLocateRsp(handle.dis);
            if (!response.isEmpty()) {
                return Optional.of(new DaemonEntry(response, keyString));
            } else {
                return Optional.empty();
            }
        }
    }

    @Override
    public String toString() {
        return "daemon (distance=" + distance + " socketPath=" + socketPath + ")";
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

    private class DaemonEntry extends EntrySupport implements Entry {
        private final String keyString;

        private DaemonEntry(Map<String, String> metadata, String keyString) {
            super(metadata);
            this.keyString = keyString;
        }

        @Override
        public void transferTo(Path file) throws IOException {
            logger.debug("TRANSFER '{}'->'{}'", keyString, file);
            try (Handle handle = create()) {
                writeTransferReq(handle.dos, keyString, file.toString());
                readTransferRsp(handle.dis);
            }
        }
    }
}
