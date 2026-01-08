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
import static eu.maveniverse.maven.mimir.daemon.protocol.Request.CMD_PRESEED;
import static eu.maveniverse.maven.mimir.daemon.protocol.Request.CMD_STORE_PATH;
import static eu.maveniverse.maven.mimir.daemon.protocol.Request.CMD_TRANSFER;
import static eu.maveniverse.maven.mimir.daemon.protocol.Request.DATA_GAV_ITSELF;
import static eu.maveniverse.maven.mimir.shared.impl.EntryUtils.mergeEntry;
import static eu.maveniverse.maven.mimir.shared.impl.EntryUtils.splitChecksums;
import static eu.maveniverse.maven.mimir.shared.impl.EntryUtils.splitMetadata;

import eu.maveniverse.maven.mimir.daemon.protocol.Handle;
import eu.maveniverse.maven.mimir.daemon.protocol.ImmutableResponse;
import eu.maveniverse.maven.mimir.daemon.protocol.Request;
import eu.maveniverse.maven.mimir.daemon.protocol.Response;
import eu.maveniverse.maven.mimir.daemon.protocol.Session;
import eu.maveniverse.maven.mimir.shared.impl.node.CachingSystemNode;
import eu.maveniverse.maven.mimir.shared.node.Entry;
import eu.maveniverse.maven.mimir.shared.node.LocalEntry;
import eu.maveniverse.maven.shared.core.component.ComponentSupport;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

final class DaemonServer extends ComponentSupport implements Runnable {
    private final Handle handle;
    private final Map<String, String> daemonData;
    private final CachingSystemNode cachingSystemNode;
    private final Predicate<Request> clientPredicate;
    private final Function<Path, Boolean> preseedItself;
    private final BiFunction<Path, String, Boolean> preseedGAVS;
    private final Runnable shutdownHook;
    private final Map<String, Map<String, String>> sessions;

    DaemonServer(
            Handle handle,
            Map<String, String> daemonData,
            CachingSystemNode cachingSystemNode,
            Predicate<Request> clientPredicate,
            Function<Path, Boolean> preseedItself,
            BiFunction<Path, String, Boolean> preseedGAVS,
            Runnable shutdownHook) {
        this.handle = handle;
        this.daemonData = daemonData;
        this.cachingSystemNode = cachingSystemNode;
        this.clientPredicate = clientPredicate;
        this.preseedItself = preseedItself;
        this.preseedGAVS = preseedGAVS;
        this.shutdownHook = shutdownHook;
        this.sessions = new HashMap<>();
    }

    @Override
    public void run() {
        try (handle) {
            Thread.currentThread().setName("DVT");
            Request request = handle.readRequest();
            try {
                switch (request.cmd()) {
                    case CMD_HELLO -> {
                        if (clientPredicate.test(request)) {
                            String sessionId = UUID.randomUUID().toString();
                            Map<String, String> session = new HashMap<>();
                            session.put(Session.SESSION_ID, sessionId);
                            handle.writeResponse(ImmutableResponse.builder()
                                    .status(Response.STATUS_OK)
                                    .session(session)
                                    .data(daemonData)
                                    .build());
                            sessions.put(sessionId, request.data());
                            logger.debug("{} {} > {}", request.cmd(), request.data(), sessionId);
                        } else {
                            handle.writeResponse(Response.koMessage(request, "Bad client; align both versions"));
                            logger.debug("{} {} > REJECT", request.cmd(), request.data());
                        }
                    }
                    case CMD_BYE -> {
                        String sessionId = request.session().get(Session.SESSION_ID);
                        if (sessionId != null) {
                            sessions.remove(sessionId);
                        }
                        logger.debug("{} {} < {}", request.cmd(), request.data(), sessionId);
                        handle.writeResponse(Response.okMessage(request, "So Long, and Thanks for All the Fish"));
                        if (Boolean.parseBoolean(request.data().getOrDefault(Request.DATA_SHUTDOWN, "false"))) {
                            shutdownHook.run();
                        }
                    }
                    case CMD_LOCATE -> {
                        String keyString = request.requireData(Request.DATA_KEYSTRING);
                        URI key = URI.create(keyString);
                        Optional<? extends Entry> entry = cachingSystemNode.locate(key);
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
                        Optional<? extends LocalEntry> entry = cachingSystemNode.locate(key);
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
                        logger.debug("{} -> {}", request.cmd(), cachingSystemNode.checksumAlgorithms());
                        LinkedHashMap<String, String> data = new LinkedHashMap<>();
                        cachingSystemNode.checksumAlgorithms().forEach(c -> data.put(c, c));
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
                                mergeEntry(cachingSystemNode.store(
                                        key, Path.of(pathString), splitMetadata(data), splitChecksums(data)))));
                    }
                    case CMD_PRESEED -> {
                        String gavs = request.requireData(Request.DATA_GAVS);
                        String pathString = request.requireData(Request.DATA_PATHSTRING);
                        logger.debug("{} {} <- {}", request.cmd(), gavs, pathString);
                        boolean result = false;
                        String errorMessage = null;
                        try {
                            Path path = Path.of(pathString);
                            if (!path.isAbsolute()) {
                                throw new IllegalArgumentException("path must be absolute");
                            }
                            if (DATA_GAV_ITSELF.equals(gavs)) {
                                result = preseedItself.apply(path);
                            } else {
                                result = preseedGAVS.apply(path, gavs);
                            }
                        } catch (Exception e) {
                            logger.warn("PreSeed failure", e);
                            errorMessage = e.getMessage();
                        }
                        if (result) {
                            handle.writeResponse(Response.okData(request, Map.of()));
                        } else {
                            if (errorMessage != null) {
                                handle.writeResponse(
                                        Response.koMessage(request, "Bad preseed request: " + errorMessage));
                            } else {
                                handle.writeResponse(Response.koMessage(request, "Preseed failed"));
                            }
                        }
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
