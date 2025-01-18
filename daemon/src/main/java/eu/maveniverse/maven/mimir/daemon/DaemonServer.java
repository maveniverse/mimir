/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.daemon;

import static eu.maveniverse.maven.mimir.shared.impl.SimpleProtocol.CMD_LOCATE;
import static eu.maveniverse.maven.mimir.shared.impl.SimpleProtocol.CMD_TRANSFER;
import static eu.maveniverse.maven.mimir.shared.impl.SimpleProtocol.writeLocateRspOK;
import static eu.maveniverse.maven.mimir.shared.impl.SimpleProtocol.writeRspKO;
import static eu.maveniverse.maven.mimir.shared.impl.SimpleProtocol.writeTransferRspOK;

import eu.maveniverse.maven.mimir.shared.node.Entry;
import eu.maveniverse.maven.mimir.shared.node.LocalEntry;
import eu.maveniverse.maven.mimir.shared.node.LocalNode;
import eu.maveniverse.maven.mimir.shared.node.RemoteNode;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.URI;
import java.nio.channels.Channels;
import java.nio.channels.SocketChannel;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DaemonServer implements Runnable {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final SocketChannel socketChannel;
    private final DataOutputStream dos;
    private final DataInputStream dis;

    private final LocalNode localNode;
    private final List<RemoteNode> remoteNodes;

    public DaemonServer(SocketChannel socketChannel, LocalNode localNode, List<RemoteNode> remoteNodes) {
        this.socketChannel = socketChannel;
        this.dos = new DataOutputStream(Channels.newOutputStream(socketChannel));
        this.dis = new DataInputStream(Channels.newInputStream(socketChannel));
        this.localNode = localNode;
        this.remoteNodes = remoteNodes;
    }

    @Override
    public void run() {
        try (socketChannel) {
            String cmd = dis.readUTF();
            switch (cmd) {
                case CMD_LOCATE -> {
                    String keyString = dis.readUTF();
                    logger.debug("{} {}", cmd, keyString);
                    URI key = URI.create(keyString);
                    Optional<? extends Entry> entry = localNode.locate(key);
                    if (entry.isEmpty()) {
                        for (RemoteNode node : remoteNodes) {
                            entry = node.locate(key);
                            if (entry.isPresent()) {
                                localNode.store(key, entry.orElseThrow());
                                break;
                            }
                        }
                    }
                    if (entry.isPresent()) {
                        writeLocateRspOK(dos, entry.orElseThrow().metadata());
                    } else {
                        writeLocateRspOK(dos, Collections.emptyMap());
                    }
                }
                case CMD_TRANSFER -> {
                    String keyString = dis.readUTF();
                    String pathString = dis.readUTF();
                    logger.debug("{} {} {}", cmd, keyString, pathString);
                    URI key = URI.create(keyString);
                    Path path = Path.of(pathString);
                    Optional<LocalEntry> entry = localNode.locate(key);
                    if (entry.isPresent()) {
                        entry.orElseThrow().transferTo(path);
                        writeTransferRspOK(dos);
                    } else {
                        writeRspKO(dos, "Not found");
                    }
                }
            }
        } catch (Exception e) {
            try {
                writeRspKO(dos, e.getMessage());
            } catch (Exception ignored) {
            }
            logger.warn("Server error", e);
        }
    }
}
