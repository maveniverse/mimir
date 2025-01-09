/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.node.daemon;

import eu.maveniverse.maven.mimir.shared.CacheEntry;
import eu.maveniverse.maven.mimir.shared.CacheKey;
import eu.maveniverse.maven.mimir.shared.node.Node;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.SocketChannel;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This node is delegating all the work to daemon via Unix Domain Sockets.
 * Locate asks for key, and daemon tells "OK" or "KO". If "OK" daemon also immediately
 * pulls the file to local cache. Next, cache entry "transferTo" tells daemon where to
 * transfer to.
 */
public class UdsNode implements Node {
    public static final class Handle implements Closeable {
        private final SocketChannel socketChannel;
        private final DataOutputStream dos;
        private final DataInputStream dis;

        public Handle(SocketChannel socketChannel) {
            this.socketChannel = socketChannel;
            this.dos = new DataOutputStream(new BufferedOutputStream(Channels.newOutputStream(socketChannel)));
            this.dis = new DataInputStream(new BufferedInputStream(Channels.newInputStream(socketChannel)));
        }

        public String locate(String cacheKey) throws IOException {
            dos.writeUTF("LOCATE");
            dos.writeUTF(cacheKey);
            dos.flush();
            return dis.readUTF();
        }

        public String transferTo(String cacheKey, String path) throws IOException {
            dos.writeUTF("TRANSFER");
            dos.writeUTF(cacheKey);
            dos.writeUTF(path);
            dos.flush();
            return dis.readUTF();
        }

        @Override
        public void close() throws IOException {
            dos.writeUTF("BYE");
            dos.flush();
            socketChannel.close();
        }
    }

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final Supplier<Handle> commSupplier;

    public UdsNode(Supplier<Handle> commSupplier) {
        this.commSupplier = commSupplier;
    }

    @Override
    public String name() {
        return UdsNodeConfig.NAME;
    }

    @Override
    public int distance() {
        return 100;
    }

    @Override
    public Optional<CacheEntry> locate(CacheKey key) throws IOException {
        String keyString = CacheKey.toKeyString(key);
        logger.debug("LOCATE '{}'", keyString);
        try (Handle handle = commSupplier.get()) {
            String response = handle.locate(keyString);
            if (response.startsWith("OK")) {
                return Optional.of(new UdsCacheEntry(commSupplier, keyString));
            } else {
                return Optional.empty();
            }
        }
    }

    @Override
    public void close() {}

    private class UdsCacheEntry implements CacheEntry {
        private final Supplier<Handle> commSupplier;
        private final String keyString;

        private UdsCacheEntry(Supplier<Handle> commSupplier, String keyString) {
            this.commSupplier = commSupplier;
            this.keyString = keyString;
        }

        @Override
        public String origin() {
            return name();
        }

        @Override
        public void transferTo(Path file) throws IOException {
            logger.debug("TRANSFER '{}'->'{}'", keyString, file);
            try (Handle handle = commSupplier.get()) {
                String response = handle.transferTo(keyString, file.toString());
                if (!response.startsWith("OK")) {
                    throw new IOException(response);
                }
            }
        }
    }
}
