/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.daemon;

import static eu.maveniverse.maven.mimir.node.daemon.SimpleProtocol.CMD_LOCATE;
import static eu.maveniverse.maven.mimir.node.daemon.SimpleProtocol.CMD_LS_CHECKSUMS;
import static eu.maveniverse.maven.mimir.node.daemon.SimpleProtocol.CMD_STORE_PATH;
import static eu.maveniverse.maven.mimir.node.daemon.SimpleProtocol.CMD_TRANSFER;
import static eu.maveniverse.maven.mimir.node.daemon.SimpleProtocol.readMap;
import static eu.maveniverse.maven.mimir.node.daemon.SimpleProtocol.writeLocateRspOK;
import static eu.maveniverse.maven.mimir.node.daemon.SimpleProtocol.writeLsChecksumsRspOK;
import static eu.maveniverse.maven.mimir.node.daemon.SimpleProtocol.writeRspKO;
import static eu.maveniverse.maven.mimir.node.daemon.SimpleProtocol.writeStorePathRspOK;
import static eu.maveniverse.maven.mimir.node.daemon.SimpleProtocol.writeTransferRspOK;
import static eu.maveniverse.maven.mimir.shared.impl.Utils.mergeMetadataAndChecksums;

import eu.maveniverse.maven.mimir.shared.node.Entry;
import eu.maveniverse.maven.mimir.shared.node.RemoteNode;
import eu.maveniverse.maven.mimir.shared.node.SystemEntry;
import eu.maveniverse.maven.mimir.shared.node.SystemNode;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.channels.Channels;
import java.nio.channels.SocketChannel;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DaemonServer implements Runnable {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final SocketChannel socketChannel;
    private final DataOutputStream dos;
    private final DataInputStream dis;

    private final SystemNode systemNode;
    private final List<RemoteNode> remoteNodes;

    public DaemonServer(SocketChannel socketChannel, SystemNode systemNode, List<RemoteNode> remoteNodes) {
        this.socketChannel = socketChannel;
        this.dos = new DataOutputStream(Channels.newOutputStream(socketChannel));
        this.dis = new DataInputStream(Channels.newInputStream(socketChannel));
        this.systemNode = systemNode;
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
                    Optional<? extends Entry> entry = systemNode.locate(key);
                    if (entry.isEmpty()) {
                        for (RemoteNode node : remoteNodes) {
                            entry = node.locate(key);
                            if (entry.isPresent()) {
                                systemNode.store(key, entry.orElseThrow());
                                break;
                            }
                        }
                    }
                    if (entry.isPresent()) {
                        Entry entryValue = entry.orElseThrow();
                        writeLocateRspOK(dos, mergeMetadataAndChecksums(entryValue.metadata(), entryValue.checksums()));
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
                    Optional<? extends SystemEntry> entry = systemNode.locate(key);
                    if (entry.isPresent()) {
                        entry.orElseThrow().transferTo(path);
                        writeTransferRspOK(dos);
                    } else {
                        writeRspKO(dos, "Not found");
                    }
                }
                case CMD_LS_CHECKSUMS -> {
                    logger.debug("{}", cmd);
                    writeLsChecksumsRspOK(
                            dos, new ArrayList<>(systemNode.checksumFactories().keySet()));
                }
                case CMD_STORE_PATH -> {
                    String keyString = dis.readUTF();
                    String pathString = dis.readUTF();
                    Map<String, String> checksums = readMap(dis);
                    logger.debug("{} {} {}", cmd, keyString, pathString);
                    URI key = URI.create(keyString);
                    Path path = Path.of(pathString);
                    systemNode.store(key, path, checksums);
                    writeStorePathRspOK(dos);
                }
                default -> {
                    writeRspKO(dos, "Bad command");
                }
            }
        } catch (IOException e) {
            try {
                writeRspKO(dos, e.getMessage());
            } catch (Exception ignored) {
            }
            logger.warn("Server error", e);
        }
    }
}
