/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.daemon;

import static eu.maveniverse.maven.mimir.node.daemon.protocol.Request.CMD_BYE;
import static eu.maveniverse.maven.mimir.node.daemon.protocol.Request.CMD_HELLO;
import static eu.maveniverse.maven.mimir.node.daemon.protocol.Request.CMD_LOCATE;
import static eu.maveniverse.maven.mimir.node.daemon.protocol.Request.CMD_LS_CHECKSUMS;
import static eu.maveniverse.maven.mimir.node.daemon.protocol.Request.CMD_STORE_PATH;
import static eu.maveniverse.maven.mimir.node.daemon.protocol.Request.CMD_TRANSFER;
import static eu.maveniverse.maven.mimir.shared.impl.Utils.mergeEntry;
import static eu.maveniverse.maven.mimir.shared.impl.Utils.splitChecksums;
import static eu.maveniverse.maven.mimir.shared.impl.Utils.splitMetadata;

import eu.maveniverse.maven.mimir.node.daemon.protocol.Handle;
import eu.maveniverse.maven.mimir.node.daemon.protocol.ImmutableResponse;
import eu.maveniverse.maven.mimir.node.daemon.protocol.Request;
import eu.maveniverse.maven.mimir.node.daemon.protocol.Response;
import eu.maveniverse.maven.mimir.shared.node.Entry;
import eu.maveniverse.maven.mimir.shared.node.RemoteEntry;
import eu.maveniverse.maven.mimir.shared.node.RemoteNode;
import eu.maveniverse.maven.mimir.shared.node.SystemEntry;
import eu.maveniverse.maven.mimir.shared.node.SystemNode;
import java.io.IOException;
import java.net.URI;
import java.nio.channels.Channels;
import java.nio.channels.SocketChannel;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class DaemonServer implements Runnable {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final Handle handle;
    private final Map<String, String> daemonData;
    private final SystemNode<?> systemNode;
    private final List<RemoteNode<?>> remoteNodes;
    private final Runnable shutdownHook;

    DaemonServer(
            SocketChannel socketChannel,
            Map<String, String> daemonData,
            SystemNode<?> systemNode,
            List<RemoteNode<?>> remoteNodes,
            Runnable shutdownHook) {
        this.handle = new Handle(Channels.newOutputStream(socketChannel), Channels.newInputStream(socketChannel));
        this.daemonData = daemonData;
        this.systemNode = systemNode;
        this.remoteNodes = remoteNodes;
        this.shutdownHook = shutdownHook;
    }

    @Override
    public void run() {
        try (handle) {
            Thread.currentThread().setName("DVT");
            Request request = handle.readRequest();
            try {
                switch (request.cmd()) {
                    case CMD_HELLO -> {
                        logger.debug("{} {}", request.cmd(), request.data());
                        Map<String, String> session = new HashMap<>();
                        session.put(Request.SESSION_ID, "todo");
                        handle.writeResponse(ImmutableResponse.builder()
                                .status(Response.STATUS_OK)
                                .session(session)
                                .data(daemonData)
                                .build());
                    }
                    case CMD_BYE -> {
                        logger.debug("{} {}", request.cmd(), request.data());
                        handle.writeResponse(Response.okMessage(request, "So Long, and Thanks for All the Fish"));

                        if (Boolean.parseBoolean(request.data().getOrDefault(Request.DATA_SHUTDOWN, "false"))) {
                            shutdownHook.run();
                        }
                    }
                    case CMD_LOCATE -> {
                        String keyString = request.requireData(Request.DATA_KEYSTRING);
                        URI key = URI.create(keyString);
                        Optional<? extends Entry> entry = systemNode.locate(key);
                        if (entry.isEmpty()) {
                            for (RemoteNode<?> node : remoteNodes) {
                                Optional<? extends RemoteEntry> remoteEntry = node.locate(key);
                                if (remoteEntry.isPresent()) {
                                    entry = Optional.of(systemNode.store(key, remoteEntry.orElseThrow()));
                                    break;
                                }
                            }
                        }
                        logger.debug("{} {} {}", request.cmd(), entry.isPresent() ? "HIT" : "MISS", keyString);
                        if (entry.isPresent()) {
                            Entry entryValue = entry.orElseThrow();
                            handle.writeResponse(Response.okData(request, mergeEntry(entryValue)));
                        } else {
                            handle.writeResponse(Response.okData(request, Map.of()));
                        }
                    }
                    case CMD_TRANSFER -> {
                        String keyString = request.requireData(Request.DATA_KEYSTRING);
                        String pathString = request.requireData(Request.DATA_PATHSTRING);
                        URI key = URI.create(keyString);
                        Path path = Path.of(pathString);
                        Optional<? extends SystemEntry> entry = systemNode.locate(key);
                        logger.debug(
                                "{} {} {} -> {}",
                                request.cmd(),
                                entry.isPresent() ? "HIT" : "MISS",
                                keyString,
                                pathString);
                        if (entry.isPresent()) {
                            entry.orElseThrow().transferTo(path);
                            handle.writeResponse(Response.okData(request, Map.of()));
                        } else {
                            handle.writeResponse(Response.koMessage(request, "Not found"));
                        }
                    }
                    case CMD_LS_CHECKSUMS -> {
                        logger.debug("{} -> {}", request.cmd(), systemNode.checksumAlgorithms());
                        handle.writeResponse(
                                Response.okData(request, Handle.listToMap(systemNode.checksumAlgorithms())));
                    }
                    case CMD_STORE_PATH -> {
                        String keyString = request.requireData(Request.DATA_KEYSTRING);
                        String pathString = request.requireData(Request.DATA_PATHSTRING);
                        Map<String, String> data = request.data();
                        logger.debug("{} {} <- {}", request.cmd(), keyString, pathString);
                        handle.writeResponse(Response.okData(
                                request,
                                mergeEntry(systemNode.store(
                                        URI.create(keyString),
                                        Path.of(pathString),
                                        splitMetadata(data),
                                        splitChecksums(data)))));
                    }
                    default -> handle.writeResponse(Response.koMessage(request, "Bad command"));
                }
            } catch (IOException e) {
                try {
                    handle.writeResponse(Response.koMessage(request, e.getMessage()));
                } catch (Exception ignored) {
                    // fall thru
                }
                logger.warn("Server error", e);
            }
        } catch (Exception e) {
            logger.warn("Server error", e);
        }
    }
}
