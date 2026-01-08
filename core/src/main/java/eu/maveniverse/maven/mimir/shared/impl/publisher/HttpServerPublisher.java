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
import eu.maveniverse.maven.mimir.shared.impl.Executors;
import eu.maveniverse.maven.mimir.shared.node.LocalEntry;
import eu.maveniverse.maven.mimir.shared.node.LocalNode;
import eu.maveniverse.maven.shared.core.component.ComponentSupport;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;

public class HttpServerPublisher extends PublisherSupport {
    private final HttpServer httpServer;

    public HttpServerPublisher(LocalNode localNode, PublisherConfig publisherConfig) throws IOException {
        super(localNode, publisherConfig);
        httpServer = HttpServer.create(new InetSocketAddress(publisherConfig.hostPort()), 0);

        httpServer.setExecutor(Executors.executorService());
        httpServer.createContext("/txid", new TxHandler(this::publishedEntry));
        logger.info(
                "HTTP publisher starting at {} -> {}:{}",
                httpServer.getAddress(),
                publisherConfig.hostAddress(),
                httpServer.getAddress().getPort());
        httpServer.start();
    }

    @Override
    protected URI createHandle(String token) {
        return URI.create("http://" + publisherConfig.hostAddress() + ":"
                + httpServer.getAddress().getPort() + "/txid/" + token);
    }

    @Override
    protected void doClose() {
        logger.info("HTTP publisher stopping at {}", httpServer.getAddress());
        httpServer.stop(0);
    }

    @Override
    public String toString() {
        return "HTTP(" + publisherConfig.hostAddress() + ":"
                + httpServer.getAddress().getPort() + ")";
    }

    private static class TxHandler extends ComponentSupport implements HttpHandler {
        private final Function<String, Optional<LocalEntry>> entrySupplier;

        private final DateTimeFormatter rfc7231 = DateTimeFormatter.ofPattern(
                        "EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH)
                .withZone(ZoneId.of("GMT"));

        public TxHandler(Function<String, Optional<LocalEntry>> entrySupplier) {
            this.entrySupplier = requireNonNull(entrySupplier, "entrySupplier");
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                String ctxPath = exchange.getHttpContext().getPath();
                String path = exchange.getRequestURI().getPath();
                if ("GET".equals(exchange.getRequestMethod()) && path.length() > ctxPath.length()) {
                    String token = exchange.getRequestURI().getPath().substring(ctxPath.length() + 1);
                    Optional<LocalEntry> entry = entrySupplier.apply(token);
                    if (entry.isPresent()) {
                        LocalEntry localEntry = entry.orElseThrow();
                        Headers headers = exchange.getResponseHeaders();
                        headers.add("Last-Modified", rfc7231.format(localEntry.getContentLastModified()));
                        headers.add("Content-Type", "application/octet-stream");
                        logger.debug("HIT {} to {}", token, exchange.getRemoteAddress());
                        exchange.sendResponseHeaders(200, localEntry.getContentLength());
                        try (OutputStream os = exchange.getResponseBody();
                                InputStream is = localEntry.inputStream()) {
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
