/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.daemon;

import static eu.maveniverse.maven.mimir.daemon.protocol.Request.CMD_BYE;
import static eu.maveniverse.maven.mimir.daemon.protocol.Request.CMD_HELLO;
import static eu.maveniverse.maven.mimir.daemon.protocol.Request.CMD_LOCATE;
import static eu.maveniverse.maven.mimir.daemon.protocol.Request.CMD_LS_CHECKSUMS;
import static eu.maveniverse.maven.mimir.daemon.protocol.Request.CMD_STORE_PATH;
import static eu.maveniverse.maven.mimir.daemon.protocol.Request.CMD_TRANSFER;
import static eu.maveniverse.maven.mimir.shared.impl.Utils.mergeEntry;
import static eu.maveniverse.maven.mimir.shared.impl.Utils.splitChecksums;
import static eu.maveniverse.maven.mimir.shared.impl.Utils.splitMetadata;

import eu.maveniverse.maven.mimir.daemon.protocol.Handle;
import eu.maveniverse.maven.mimir.daemon.protocol.ImmutableResponse;
import eu.maveniverse.maven.mimir.daemon.protocol.Request;
import eu.maveniverse.maven.mimir.daemon.protocol.Response;
import eu.maveniverse.maven.mimir.daemon.protocol.Session;
import eu.maveniverse.maven.mimir.shared.node.Entry;
import eu.maveniverse.maven.mimir.shared.node.RemoteEntry;
import eu.maveniverse.maven.mimir.shared.node.RemoteNode;
import eu.maveniverse.maven.mimir.shared.node.SystemEntry;
import eu.maveniverse.maven.mimir.shared.node.SystemNode;
import eu.maveniverse.maven.shared.core.component.ComponentSupport;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

final class DaemonServer extends ComponentSupport implements Runnable {
    private final Handle handle;
    private final Map<String, String> daemonData;
    private final SystemNode<?> systemNode;
    private final List<RemoteNode<?>> remoteNodes;
    private final Predicate<Request> clientPredicate;
    private final Runnable shutdownHook;

    DaemonServer(
            Handle handle,
            Map<String, String> daemonData,
            SystemNode<?> systemNode,
            List<RemoteNode<?>> remoteNodes,
            Predicate<Request> clientPredicate,
            Runnable shutdownHook) {
        this.handle = handle;
        this.daemonData = daemonData;
        this.systemNode = systemNode;
        this.remoteNodes = remoteNodes;
        this.clientPredicate = clientPredicate;
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
                        if (clientPredicate.test(request)) {
                            Map<String, String> session = new HashMap<>();
                            session.put(Session.SESSION_ID, UUID.randomUUID().toString());
                            handle.writeResponse(ImmutableResponse.builder()
                                    .status(Response.STATUS_OK)
                                    .session(session)
                                    .data(daemonData)
                                    .build());
                        } else {
                            handle.writeResponse(Response.koMessage(request, "Bad client; align both versions"));
                        }
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
                        LinkedHashMap<String, String> data = new LinkedHashMap<>();
                        systemNode.checksumAlgorithms().forEach(c -> data.put(c, c));
                        handle.writeResponse(Response.okData(request, data));
                    }
                    case CMD_STORE_PATH -> {
                        String keyString = request.requireData(Request.DATA_KEYSTRING);
                        String pathString = request.requireData(Request.DATA_PATHSTRING);
                        Map<String, String> data = request.data();
                        logger.debug("{} {} <- {}", request.cmd(), keyString, pathString);
                        URI key = URI.create(keyString);
                        handle.writeResponse(Response.okData(
                                request,
                                mergeEntry(systemNode.store(
                                        key, Path.of(pathString), splitMetadata(data), splitChecksums(data)))));
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
