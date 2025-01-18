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
import eu.maveniverse.maven.mimir.shared.node.RemoteEntry;
import eu.maveniverse.maven.mimir.shared.node.RemoteNode;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.channels.Channels;
import java.nio.channels.SocketChannel;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This node is delegating all the work to daemon via Unix Domain Sockets.
 */
public class DaemonNode extends NodeSupport implements RemoteNode {
    public static final class Handle implements Closeable {
        private final SocketChannel socketChannel;
        private final DataOutputStream dos;
        private final DataInputStream dis;

        public Handle(SocketChannel socketChannel) {
            this.socketChannel = socketChannel;
            this.dos = new DataOutputStream(new BufferedOutputStream(Channels.newOutputStream(socketChannel)));
            this.dis = new DataInputStream(new BufferedInputStream(Channels.newInputStream(socketChannel)));
        }

        @Override
        public void close() throws IOException {
            socketChannel.close();
        }
    }

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final Supplier<Handle> commSupplier;

    public DaemonNode(Supplier<Handle> commSupplier) {
        super(DaemonConfig.NAME, 10);
        this.commSupplier = requireNonNull(commSupplier, "commSupplier");
    }

    @Override
    public Optional<Entry> locate(URI key) throws IOException {
        String keyString = key.toASCIIString();
        logger.debug("LOCATE '{}'", keyString);
        try (Handle handle = commSupplier.get()) {
            writeLocateReq(handle.dos, keyString);
            Map<String, String> response = readLocateRsp(handle.dis);
            if (!response.isEmpty()) {
                return Optional.of(new DaemonEntry(this, response, commSupplier, keyString));
            } else {
                return Optional.empty();
            }
        }
    }

    private class DaemonEntry extends EntrySupport implements RemoteEntry {
        private final Supplier<Handle> commSupplier;
        private final String keyString;

        private DaemonEntry(
                DaemonNode node, Map<String, String> metadata, Supplier<Handle> commSupplier, String keyString) {
            super(node, metadata);
            this.commSupplier = commSupplier;
            this.keyString = keyString;
        }

        @Override
        public void transferTo(Path file) throws IOException {
            logger.debug("TRANSFER '{}'->'{}'", keyString, file);
            try (Handle handle = commSupplier.get()) {
                writeTransferReq(handle.dos, keyString, file.toString());
                readTransferRsp(handle.dis);
            }
        }
    }
}
