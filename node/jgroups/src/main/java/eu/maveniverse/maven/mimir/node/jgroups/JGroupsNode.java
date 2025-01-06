/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.node.jgroups;

import eu.maveniverse.maven.mimir.shared.CacheEntry;
import eu.maveniverse.maven.mimir.shared.CacheKey;
import eu.maveniverse.maven.mimir.shared.Config;
import eu.maveniverse.maven.mimir.shared.impl.LocalNodeFactoryImpl;
import eu.maveniverse.maven.mimir.shared.node.LocalCacheEntry;
import eu.maveniverse.maven.mimir.shared.node.LocalNode;
import eu.maveniverse.maven.mimir.shared.node.Node;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.Channels;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.ObjectMessage;
import org.jgroups.blocks.MessageDispatcher;
import org.jgroups.blocks.RequestHandler;
import org.jgroups.blocks.RequestOptions;
import org.jgroups.blocks.Response;
import org.jgroups.util.RspList;
import org.jgroups.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JGroupsNode implements Node, RequestHandler {
    public static void main(String... args) throws Exception {
        Logger logger = LoggerFactory.getLogger(JGroupsNode.class);

        Path basedir = null;
        String nodeName = null;
        if (args.length > 0) {
            basedir = Config.getCanonicalPath(Path.of(args[0]));
        }
        if (args.length > 1) {
            nodeName = args[1];
        }

        HashMap<String, String> config = new HashMap<>();
        if (basedir != null) {
            config.put("mimir.local.basedir", basedir.toString());
        }
        if (nodeName != null) {
            config.put("mimir.local.name", nodeName);
        }
        LocalNode localNode = new LocalNodeFactoryImpl()
                .createLocalNode(Config.defaults().userProperties(config).build());
        JGroupsNode publisher = new JGroupsNode(
                localNode,
                new JChannel("udp-new.xml")
                        .name(nodeName)
                        .setDiscardOwnMessages(true)
                        .connect("mimir-jgroups"),
                true);

        logger.info("");
        logger.info("JGroupsNode publisher started (Ctrl+C to exit)");
        logger.info("Publishing:");
        logger.info("* {} ({})", localNode.name(), localNode.basedir());
        try {
            new CountDownLatch(1).await(); // this is merely to get interrupt
        } catch (InterruptedException e) {
            publisher.close();
        }
    }

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final LocalNode localNode;
    private final JChannel channel;
    private final MessageDispatcher messageDispatcher;

    private final ServerSocket serverSocket;
    private final ConcurrentHashMap<String, LocalCacheEntry> tx;
    private final ExecutorService executor;

    public JGroupsNode(LocalNode localNode, JChannel channel, boolean publisher) throws IOException {
        this.localNode = localNode;
        this.channel = channel;
        if (publisher) {
            this.messageDispatcher = new MessageDispatcher(channel, this);
            this.serverSocket = new ServerSocket(0, 50, InetAddress.getLocalHost());
            this.tx = new ConcurrentHashMap<>();
            this.executor = Executors.newFixedThreadPool(6);

            Thread serverThread = new Thread(() -> {
                try {
                    while (true) {
                        Socket accepted = serverSocket.accept();
                        executor.submit(() -> {
                            try (Socket socket = accepted) {
                                byte[] buf = socket.getInputStream().readNBytes(36);
                                OutputStream out = socket.getOutputStream();
                                if (buf.length == 36) {
                                    String txid = new String(buf, StandardCharsets.UTF_8);
                                    LocalCacheEntry cacheEntry = tx.remove(txid);
                                    if (cacheEntry != null) {
                                        logger.debug("SERVER HIT: {} to {}", txid, socket.getRemoteSocketAddress());
                                        Channels.newInputStream(cacheEntry.getReadFileChannel())
                                                .transferTo(out);
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
                } catch (Exception e) {
                    logger.error("Error while accepting client connection", e);
                    try {
                        close();
                    } catch (Exception ignore) {
                    }
                }
            });
            serverThread.setDaemon(true);
            serverThread.start();
        } else {
            this.messageDispatcher = new MessageDispatcher(channel);
            this.serverSocket = null;
            this.tx = null;
            this.executor = null;
        }
    }

    @Override
    public String name() {
        return JGroupsNodeFactory.NAME;
    }

    @Override
    public int distance() {
        return 500;
    }

    @Override
    public Optional<CacheEntry> locate(CacheKey key) throws IOException {
        String cmd = CMD_LOOKUP + CacheKey.toKeyString(key);
        try {
            RspList<String> responses =
                    messageDispatcher.castMessage(null, new ObjectMessage(null, cmd), RequestOptions.SYNC());
            for (String response : responses.getResults()) {
                if (response != null && response.startsWith(RSP_LOOKUP_OK)) {
                    String body = response.substring(RSP_LOOKUP_OK.length());
                    String[] parts = body.split(" ");
                    if (parts.length == 2) {
                        int colon = parts[0].indexOf(':');
                        String host = parts[0].substring(0, colon);
                        int port = Integer.parseInt(parts[0].substring(colon + 1));
                        return Optional.of(localNode.store(key, new JGroupsCacheEntry(name(), host, port, parts[1])));
                    }
                }
            }
        } catch (Exception e) {
            throw new IOException("Failed to locate", e);
        }
        return Optional.empty();
    }

    public static final String CMD_LOOKUP = "LOOKUP ";
    public static final String RSP_LOOKUP_OK = "OK ";
    public static final String RSP_LOOKUP_KO = "KO ";

    @Override
    public Object handle(Message msg) throws Exception {
        AtomicReference<Object> resp = new AtomicReference<>(null);
        AtomicBoolean respIsException = new AtomicBoolean(false);
        Response response = new Response() {
            @Override
            public void send(Object reply, boolean is_exception) {
                resp.set(reply);
                respIsException.set(is_exception);
            }

            @Override
            public void send(Message reply, boolean is_exception) {
                resp.set(reply);
                respIsException.set(is_exception);
            }
        };
        handle(msg, response);
        if (respIsException.get()) {
            throw new IllegalArgumentException(String.valueOf(resp.get()));
        }
        return resp.get();
    }

    @Override
    public void handle(Message msg, Response response) throws Exception {
        String cmd = msg.getObject();
        if (cmd != null && cmd.startsWith(CMD_LOOKUP)) {
            String keyString = cmd.substring(CMD_LOOKUP.length());
            CacheKey key = CacheKey.fromKeyString(keyString);
            Optional<CacheEntry> entry = localNode.locate(key);
            if (entry.isPresent()) {
                String txid = UUID.randomUUID().toString();
                tx.put(txid, (LocalCacheEntry) entry.orElseThrow());
                response.send(
                        RSP_LOOKUP_OK + serverSocket.getInetAddress().getHostAddress() + ":"
                                + serverSocket.getLocalPort() + " " + txid,
                        false);
                logger.info("LOOKUP OK: {} asked {}", msg.getSrc(), keyString);
                return;
            } else {
                response.send(RSP_LOOKUP_KO, false);
                logger.info("LOOKUP KO: {} asked {}", msg.getSrc(), keyString);
                return;
            }
        }
        logger.info("UNKNOWN COMMAND: {}", cmd);
        response.send("Unknown command", true);
    }

    @Override
    public void close() throws Exception {
        if (executor != null) {
            executor.shutdown();
        }
        if (serverSocket != null) {
            serverSocket.close();
        }
        messageDispatcher.close();
        channel.close();
    }

    private record JGroupsCacheEntry(String origin, String host, int port, String txid) implements CacheEntry {
        @Override
        public void transferTo(Path file) throws IOException {
            try (Socket socket = new Socket(host, port)) {
                OutputStream os = socket.getOutputStream();
                os.write(txid.getBytes(StandardCharsets.UTF_8));
                os.flush();
                Files.copy(socket.getInputStream(), file, StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }
}
