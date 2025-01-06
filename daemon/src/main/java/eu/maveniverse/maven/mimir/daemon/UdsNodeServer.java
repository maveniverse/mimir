/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.daemon;

import eu.maveniverse.maven.mimir.shared.CacheEntry;
import eu.maveniverse.maven.mimir.shared.CacheKey;
import eu.maveniverse.maven.mimir.shared.node.LocalNode;
import eu.maveniverse.maven.mimir.shared.node.Node;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.nio.channels.Channels;
import java.nio.channels.SocketChannel;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UdsNodeServer implements Runnable {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final SocketChannel socketChannel;
    private final DataOutputStream dos;
    private final DataInputStream dis;

    private final LocalNode localNode;
    private final List<Node> nodes;

    public UdsNodeServer(SocketChannel socketChannel, LocalNode localNode, List<Node> nodes) {
        this.socketChannel = socketChannel;
        this.dos = new DataOutputStream(Channels.newOutputStream(socketChannel));
        this.dis = new DataInputStream(Channels.newInputStream(socketChannel));
        this.localNode = localNode;
        this.nodes = nodes;
    }

    @Override
    public void run() {
        try (socketChannel) {
            while (true) {
                String cmd = dis.readUTF();
                switch (cmd) {
                    case "LOCATE" -> {
                        String keyString = dis.readUTF();
                        logger.debug("LOCATE {}", keyString);
                        CacheKey key = CacheKey.fromKeyString(keyString);
                        Optional<CacheEntry> entry = localNode.locate(key);
                        boolean found = entry.isPresent();
                        if (!found) {
                            for (Node node : nodes) {
                                entry = node.locate(key);
                                if (entry.isPresent()) {
                                    localNode.store(key, entry.orElseThrow());
                                    found = true;
                                    break;
                                }
                            }
                        }
                        if (found) {
                            dos.writeUTF("OK");
                        } else {
                            dos.writeUTF("KO");
                        }
                        dos.flush();
                    }
                    case "TRANSFER" -> {
                        String keyString = dis.readUTF();
                        String pathString = dis.readUTF();
                        logger.debug("TRANSFER {} {}", keyString, pathString);
                        CacheKey key = CacheKey.fromKeyString(keyString);
                        Path path = Path.of(pathString);
                        Optional<CacheEntry> entry = localNode.locate(key);
                        try {
                            try (CacheEntry e = entry.orElseThrow(() -> new IllegalStateException("Entry not found"))) {
                                e.transferTo(path);
                            }
                            dos.writeUTF("OK");
                            dos.flush();
                        } catch (Exception e) {
                            dos.writeUTF("KO " + e.getMessage());
                            dos.flush();
                        }
                    }
                    case "BYE" -> {
                        logger.debug("BYE");
                        return;
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Server error", e);
        }
    }
}
