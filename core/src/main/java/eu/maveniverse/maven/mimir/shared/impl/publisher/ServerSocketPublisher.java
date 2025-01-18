/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.shared.impl.publisher;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.mimir.shared.node.LocalEntry;
import eu.maveniverse.maven.mimir.shared.publisher.Publisher;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerSocketPublisher implements Publisher {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final ServerSocket serverSocket;
    private final ExecutorService executor;

    public ServerSocketPublisher(
            InetSocketAddress inetSocketAddress, Function<String, Optional<LocalEntry>> entrySupplier)
            throws IOException {
        requireNonNull(inetSocketAddress, "inetSocketAddress");
        requireNonNull(entrySupplier, "entrySupplier");

        this.serverSocket = new ServerSocket(inetSocketAddress.getPort(), 50, inetSocketAddress.getAddress());
        this.executor = Executors.newVirtualThreadPerTaskExecutor();

        Thread serverThread = new Thread(() -> {
            try {
                while (!serverSocket.isClosed()) {
                    Socket accepted = serverSocket.accept();
                    executor.submit(() -> {
                        try (Socket socket = accepted) {
                            byte[] buf = socket.getInputStream().readNBytes(36);
                            OutputStream out = socket.getOutputStream();
                            if (buf.length == 36) {
                                String txid = new String(buf, StandardCharsets.UTF_8);
                                Optional<LocalEntry> entry = entrySupplier.apply(txid);
                                if (entry.isPresent()) {
                                    logger.debug("SERVER HIT: {} to {}", txid, socket.getRemoteSocketAddress());
                                    try (InputStream inputStream =
                                            entry.orElseThrow().openStream()) {
                                        inputStream.transferTo(out);
                                    }
                                } else {
                                    logger.warn("SERVER MISS: {} to {}", txid, socket.getRemoteSocketAddress());
                                }
                            }
                            out.flush();
                        } catch (Exception e) {
                            logger.error("Error while serving a client", e);
                        }
                    });
                }
            } catch (SocketException ignored) {
                // closed
            } catch (Exception e) {
                logger.error("Error while accepting client connection", e);
            } finally {
                try {
                    close();
                } catch (Exception ignore) {
                }
            }
        });
        serverThread.setDaemon(true);
        logger.info("Socket publisher starting at {}", serverSocket.getLocalSocketAddress());
        serverThread.start();
    }

    @Override
    public URI createHandle(String txid) throws IOException {
        return URI.create("socket://" + InetAddress.getLocalHost().getHostAddress() + ":" + serverSocket.getLocalPort()
                + "/" + txid);
    }

    @Override
    public void close() throws IOException {
        logger.info("Socket publisher stopping at {}", serverSocket.getLocalSocketAddress());
        executor.shutdown();
        serverSocket.close();
    }
}
