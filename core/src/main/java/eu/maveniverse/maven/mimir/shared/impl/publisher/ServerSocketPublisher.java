/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.shared.impl.publisher;

import eu.maveniverse.maven.mimir.shared.node.SystemEntry;
import eu.maveniverse.maven.mimir.shared.node.SystemNode;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerSocketPublisher extends PublisherSupport {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final ServerSocket serverSocket;
    private final ExecutorService executor;

    public ServerSocketPublisher(SystemNode systemNode, InetSocketAddress inetSocketAddress) throws IOException {
        super(systemNode);

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
                                String token = new String(buf, StandardCharsets.UTF_8);
                                Optional<SystemEntry> entry = publishedEntry(token);
                                if (entry.isPresent()) {
                                    logger.debug("HIT: {} to {}", token, socket.getRemoteSocketAddress());
                                    try (InputStream is = entry.orElseThrow().inputStream()) {
                                        is.transferTo(out);
                                    }
                                } else {
                                    logger.warn("MISS: {} to {}", token, socket.getRemoteSocketAddress());
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
            }
        });
        serverThread.setDaemon(true);
        logger.info("Socket publisher starting at {}", serverSocket.getLocalSocketAddress());
        serverThread.start();
    }

    @Override
    protected URI createHandle(String token) throws IOException {
        return URI.create("socket://" + InetAddress.getLocalHost().getHostAddress() + ":" + serverSocket.getLocalPort()
                + "/" + token);
    }

    @Override
    public void close() throws IOException {
        logger.info("Socket publisher stopping at {}", serverSocket.getLocalSocketAddress());
        executor.shutdown();
        serverSocket.close();
    }

    @Override
    public String toString() {
        return "HTTP(" + serverSocket.getLocalSocketAddress() + ")";
    }
}
