/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.node.jgroups;

import static eu.maveniverse.maven.mimir.shared.impl.SimpleProtocol.CMD_LOCATE;
import static eu.maveniverse.maven.mimir.shared.impl.SimpleProtocol.readLocateRsp;
import static eu.maveniverse.maven.mimir.shared.impl.SimpleProtocol.writeLocateReq;
import static eu.maveniverse.maven.mimir.shared.impl.SimpleProtocol.writeLocateRspOK;
import static eu.maveniverse.maven.mimir.shared.impl.SimpleProtocol.writeRspKO;
import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.mimir.shared.impl.NodeSupport;
import eu.maveniverse.maven.mimir.shared.impl.RemoteEntrySupport;
import eu.maveniverse.maven.mimir.shared.node.Entry;
import eu.maveniverse.maven.mimir.shared.node.LocalEntry;
import eu.maveniverse.maven.mimir.shared.node.LocalNode;
import eu.maveniverse.maven.mimir.shared.node.Node;
import eu.maveniverse.maven.mimir.shared.node.RemoteNode;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.jgroups.BytesMessage;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.MessageFactory;
import org.jgroups.blocks.MessageDispatcher;
import org.jgroups.blocks.RequestHandler;
import org.jgroups.blocks.RequestOptions;
import org.jgroups.blocks.Response;
import org.jgroups.util.RspList;
import org.jgroups.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JGroupsNode extends NodeSupport implements RemoteNode, RequestHandler {
    private static final String JG_TXID = "jg-txid";
    private static final String JG_HOST = "jg-host";
    private static final String JG_PORT = "jg-port";

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final LocalNode localNode;
    private final JChannel channel;
    private final MessageDispatcher messageDispatcher;

    private final ServerSocket serverSocket;
    private final ConcurrentHashMap<String, LocalEntry> tx;
    private final ExecutorService executor;

    public JGroupsNode(LocalNode localNode, JChannel channel, boolean publisher) throws IOException {
        super(JGroupsNodeFactory.NAME, 500);
        this.localNode = localNode;
        this.channel = channel;
        if (publisher) {
            this.messageDispatcher = new MessageDispatcher(channel, this);
            this.serverSocket = new ServerSocket(0, 50, InetAddress.getLocalHost());
            this.tx = new ConcurrentHashMap<>();
            this.executor = Executors.newFixedThreadPool(6);

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
                                    LocalEntry entry = tx.remove(txid);
                                    if (entry != null) {
                                        logger.debug("SERVER HIT: {} to {}", txid, socket.getRemoteSocketAddress());
                                        try (InputStream inputStream = entry.openStream()) {
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
    public Optional<Entry> locate(URI key) throws IOException {
        ByteArrayOutputStream req = new ByteArrayOutputStream();
        writeLocateReq(new DataOutputStream(req), key.toASCIIString());
        try {
            RspList<BytesMessage> responses = messageDispatcher.castMessage(
                    null, MessageFactory.create(Message.BYTES_MSG).setArray(req.toByteArray()), RequestOptions.SYNC());
            for (BytesMessage response : responses.getResults()) {
                Map<String, String> data =
                        readLocateRsp(new DataInputStream(new ByteArrayInputStream(response.getArray())));
                if (!data.isEmpty()) {
                    String host = requireNonNull(data.get(JG_HOST), JG_HOST);
                    int port = Integer.parseInt(requireNonNull(data.get(JG_PORT), JG_PORT));
                    String txid = requireNonNull(data.get(JG_TXID), JG_TXID);
                    return Optional.of(localNode.store(key, new JGroupsEntry(this, data, host, port, txid)));
                }
            }
        } catch (Exception e) {
            throw new IOException("Failed to locate", e);
        }
        return Optional.empty();
    }

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
            throw new IOException(String.valueOf(resp.get()));
        }
        return resp.get();
    }

    @Override
    public void handle(Message msg, Response response) throws Exception {
        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(msg.getArray()));
        String cmd = dis.readUTF();
        ByteArrayOutputStream data = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(data);
        boolean exception = false;
        if (CMD_LOCATE.equals(cmd)) {
            String keyString = dis.readUTF();
            URI key = URI.create(keyString);
            Optional<LocalEntry> optionalEntry = localNode.locate(key);

            HashMap<String, String> map = new HashMap<>();
            if (optionalEntry.isPresent()) {
                LocalEntry entry = optionalEntry.orElseThrow();
                String txid = UUID.randomUUID().toString();
                tx.put(txid, entry);

                map.putAll(entry.metadata());
                map.put(JG_TXID, txid);
                map.put(JG_PORT, Integer.toString(serverSocket.getLocalPort()));
                map.put(JG_HOST, InetAddress.getLocalHost().getHostAddress());
                writeLocateRspOK(dos, map);
                logger.info("{} OK: {} asked {}", cmd, msg.getSrc(), keyString);
            } else {
                writeLocateRspOK(dos, map);
                logger.info("{} KO: {} asked {}", cmd, msg.getSrc(), keyString);
            }
        } else {
            writeRspKO(dos, "Unknown command");
            exception = true;
            logger.info("UNKNOWN COMMAND: {}", cmd);
        }
        response.send(MessageFactory.create(Message.BYTES_MSG).setArray(data.toByteArray()), exception);
    }

    @Override
    public void doClose() throws IOException {
        if (executor != null) {
            executor.shutdown();
        }
        if (serverSocket != null) {
            serverSocket.close();
        }
        messageDispatcher.close();
        channel.close();
    }

    private static class JGroupsEntry extends RemoteEntrySupport {
        private final String host;
        private final int port;
        private final String txid;

        public JGroupsEntry(Node origin, Map<String, String> metadata, String host, int port, String txid) {
            super(origin, metadata);
            this.host = host;
            this.port = port;
            this.txid = txid;
        }

        @Override
        protected void handleContent(IOConsumer consumer) throws IOException {
            try (Socket socket = new Socket(host, port)) {
                OutputStream os = socket.getOutputStream();
                os.write(txid.getBytes(StandardCharsets.UTF_8));
                os.flush();
                consumer.accept(socket.getInputStream());
            }
        }
    }
}
