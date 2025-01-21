/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.shared.impl.publisher;

import static java.util.Objects.requireNonNull;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import eu.maveniverse.maven.mimir.shared.node.Entry;
import eu.maveniverse.maven.mimir.shared.node.SystemEntry;
import eu.maveniverse.maven.mimir.shared.node.SystemNode;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpServerPublisher extends PublisherSupport {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final HttpServer httpServer;

    public HttpServerPublisher(SystemNode systemNode, InetSocketAddress inetSocketAddress) throws IOException {
        super(systemNode);
        httpServer = HttpServer.create(inetSocketAddress, 0);
        httpServer.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        httpServer.createContext("/txid", new TxHandler(this::publishedEntry));
        logger.info("HTTP publisher starting at {}", httpServer.getAddress());
        httpServer.start();
    }

    @Override
    protected URI createHandle(String token) throws IOException {
        return URI.create("http://" + InetAddress.getLocalHost().getHostAddress() + ":"
                + httpServer.getAddress().getPort() + "/txid/" + token);
    }

    @Override
    public void close() {
        logger.info("HTTP publisher stopping at {}", httpServer.getAddress());
        httpServer.stop(0);
    }

    @Override
    public String toString() {
        return "HTTP(" + httpServer.getAddress() + ")";
    }

    private static class TxHandler implements HttpHandler {
        private final Logger logger = LoggerFactory.getLogger(getClass());
        private final Function<String, Optional<SystemEntry>> entrySupplier;

        private final DateTimeFormatter rfc7231 = DateTimeFormatter.ofPattern(
                        "EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH)
                .withZone(ZoneId.of("GMT"));

        public TxHandler(Function<String, Optional<SystemEntry>> entrySupplier) {
            this.entrySupplier = requireNonNull(entrySupplier, "entrySupplier");
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                String ctxPath = exchange.getHttpContext().getPath();
                String path = exchange.getRequestURI().getPath();
                if ("GET".equals(exchange.getRequestMethod()) && path.length() > ctxPath.length()) {
                    String token = exchange.getRequestURI().getPath().substring(ctxPath.length() + 1);
                    Optional<SystemEntry> entry = entrySupplier.apply(token);
                    if (entry.isPresent()) {
                        SystemEntry systemEntry = entry.orElseThrow();
                        long contentLength = Long.parseLong(
                                requireNonNull(systemEntry.metadata().get(Entry.CONTENT_LENGTH), Entry.CONTENT_LENGTH));
                        Headers headers = exchange.getResponseHeaders();
                        String contentLastModified = systemEntry.metadata().get(Entry.CONTENT_LAST_MODIFIED);
                        if (contentLastModified != null) {
                            headers.add(
                                    "Last-Modified",
                                    rfc7231.format(Instant.ofEpochMilli(Long.parseLong(contentLastModified))));
                        }
                        headers.add("Content-Type", "application/octet-stream");
                        logger.debug("HIT {} to {}", token, exchange.getRemoteAddress());
                        exchange.sendResponseHeaders(200, contentLength);
                        try (OutputStream os = exchange.getResponseBody();
                                InputStream is = systemEntry.inputStream()) {
                            is.transferTo(os);
                        }
                    } else {
                        logger.info("MISS {} to {}", token, exchange.getRemoteAddress());
                        exchange.sendResponseHeaders(404, -1);
                    }
                } else {
                    exchange.sendResponseHeaders(405, -1);
                }
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
                throw e;
            }
        }
    }
}
