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
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.SocketChannel;
import java.nio.file.Path;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This node is delegating all the work to daemon via Unix Domain Sockets.
 * Locate asks for key, and daemon tells "OK" or "KO". If "OK" daemon also immediately
 * pulls the file to local cache. Next, cache entry "transferTo" tells daemon where to
 * transfer to.
 */
public class UdsNode implements Node {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final SocketChannel socketChannel;
    private final DataOutputStream dos;
    private final DataInputStream dis;
    private final Object monitor = new Object();

    public UdsNode(SocketChannel socketChannel) {
        this.socketChannel = socketChannel;
        this.dos = new DataOutputStream(Channels.newOutputStream(socketChannel));
        this.dis = new DataInputStream(Channels.newInputStream(socketChannel));
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
        synchronized (monitor) {
            String keyString = CacheKey.toKeyString(key);
            logger.debug("LOCATE '{}'", keyString);
            dos.writeUTF("LOCATE");
            dos.writeUTF(keyString);
            dos.flush();
            String response = dis.readUTF();
            if (response.startsWith("OK")) {
                return Optional.of(new UdsCacheEntry(keyString));
            } else {
                return Optional.empty();
            }
        }
    }

    @Override
    public void close() throws Exception {
        dos.writeUTF("BYE");
        dos.flush();
        socketChannel.close();
    }

    private class UdsCacheEntry implements CacheEntry {
        private final String keyString;

        private UdsCacheEntry(String keyString) {
            this.keyString = keyString;
        }

        @Override
        public String origin() {
            return name();
        }

        @Override
        public void transferTo(Path file) throws IOException {
            synchronized (monitor) {
                logger.debug("TRANSFER '{}'->'{}'", keyString, file);
                dos.writeUTF("TRANSFER");
                dos.writeUTF(keyString);
                dos.writeUTF(file.toString());
                dos.flush();
                String response = dis.readUTF();
                if (!response.startsWith("OK")) {
                    throw new IOException(response);
                }
            }
        }
    }
}
