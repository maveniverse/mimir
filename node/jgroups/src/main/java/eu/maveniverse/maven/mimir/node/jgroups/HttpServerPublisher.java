/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.node.jgroups;

import static java.util.Objects.requireNonNull;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import eu.maveniverse.maven.mimir.shared.node.Entry;
import eu.maveniverse.maven.mimir.shared.node.LocalEntry;
import eu.maveniverse.maven.mimir.shared.node.Node;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpServerPublisher implements Publisher {
    public static void main(String... args) throws IOException {
        Function<String, Optional<LocalEntry>> entrySupplier = t -> {
            if ("123456".equals(t)) {
                byte[] payload = "this is the payload".getBytes(StandardCharsets.UTF_8);
                HashMap<String, String> metadata = new HashMap<>();
                metadata.put(Entry.CONTENT_LENGTH, Integer.toString(payload.length));
                metadata.put(
                        Entry.CONTENT_LAST_MODIFIED, Long.toString(Instant.now().toEpochMilli()));
                LocalEntry localEntry = new LocalEntry() {
                    @Override
                    public InputStream openStream() throws IOException {
                        return new ByteArrayInputStream(payload);
                    }

                    @Override
                    public Node origin() {
                        return null;
                    }

                    @Override
                    public Map<String, String> metadata() {
                        return Map.copyOf(metadata);
                    }

                    @Override
                    public void transferTo(Path file) throws IOException {
                        throw new UnsupportedOperationException("not implemented");
                    }
                };
                return Optional.of(localEntry);
            } else {
                return Optional.empty();
            }
        };
        HttpServerPublisher publisher = new HttpServerPublisher(new InetSocketAddress(0), entrySupplier);
    }

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final HttpServer httpServer;

    public HttpServerPublisher(
            InetSocketAddress inetSocketAddress, Function<String, Optional<LocalEntry>> entrySupplier)
            throws IOException {
        requireNonNull(inetSocketAddress, "inetSocketAddress");
        requireNonNull(entrySupplier, "entrySupplier");

        httpServer = HttpServer.create(inetSocketAddress, 0);
        httpServer.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        httpServer.createContext("/txid", new MimirTxHandler(entrySupplier));
        httpServer.start();

        logger.info("HTTP server started at {}", httpServer.getAddress());
    }

    @Override
    public URI createHandle(String txid) {
        return URI.create("http://xxx.xxx.xxx.xxx:" + httpServer.getAddress().getPort() + "/txid/" + txid);
    }

    @Override
    public void close() {
        httpServer.stop(5);
    }

    private static class MimirTxHandler implements HttpHandler {
        private final Logger logger = LoggerFactory.getLogger(getClass());
        private final Function<String, Optional<LocalEntry>> entrySupplier;

        private final DateTimeFormatter rfc7231 = DateTimeFormatter.ofPattern(
                        "EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH)
                .withZone(ZoneId.of("GMT"));

        public MimirTxHandler(Function<String, Optional<LocalEntry>> entrySupplier) {
            this.entrySupplier = requireNonNull(entrySupplier, "entrySupplier");
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                String ctxPath = exchange.getHttpContext().getPath();
                String path = exchange.getRequestURI().getPath();
                if ("GET".equals(exchange.getRequestMethod()) && path.length() > ctxPath.length()) {
                    String txid = exchange.getRequestURI().getPath().substring(ctxPath.length() + 1);
                    Optional<LocalEntry> entry = entrySupplier.apply(txid);
                    if (entry.isPresent()) {
                        LocalEntry localEntry = entry.orElseThrow();
                        long contentLength = Long.parseLong(
                                requireNonNull(localEntry.metadata().get(Entry.CONTENT_LENGTH), Entry.CONTENT_LENGTH));
                        Headers headers = exchange.getResponseHeaders();
                        String contentLastModified = localEntry.metadata().get(Entry.CONTENT_LAST_MODIFIED);
                        if (contentLastModified != null) {
                            headers.add(
                                    "Last-Modified",
                                    rfc7231.format(Instant.ofEpochMilli(Long.parseLong(contentLastModified))));
                        }
                        headers.add("Content-Type", "application/octet-stream");
                        headers.add("Content-Length", Long.toString(contentLength));
                        logger.debug("HIT {}", txid);
                        exchange.sendResponseHeaders(200, contentLength);
                        try (OutputStream os = exchange.getResponseBody();
                                InputStream is = localEntry.openStream()) {
                            is.transferTo(os);
                        }
                    } else {
                        logger.info("MISS {}", txid);
                        exchange.sendResponseHeaders(404, -1);
                    }
                } else {
                    exchange.sendResponseHeaders(405, -1);
                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }
    }
}
