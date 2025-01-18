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

import eu.maveniverse.maven.mimir.shared.Config;
import eu.maveniverse.maven.mimir.shared.impl.NodeSupport;
import eu.maveniverse.maven.mimir.shared.impl.publisher.PublisherRemoteEntry;
import eu.maveniverse.maven.mimir.shared.node.Entry;
import eu.maveniverse.maven.mimir.shared.node.LocalEntry;
import eu.maveniverse.maven.mimir.shared.node.LocalNode;
import eu.maveniverse.maven.mimir.shared.node.RemoteNode;
import eu.maveniverse.maven.mimir.shared.publisher.Publisher;
import eu.maveniverse.maven.mimir.shared.publisher.PublisherFactory;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
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
    private static final String PUBLISHER_HANDLE = "handle";

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final LocalNode localNode;
    private final JChannel channel;
    private final MessageDispatcher messageDispatcher;
    private final Publisher publisher;
    private final ConcurrentHashMap<String, LocalEntry> tx;

    /**
     * Creates JGroups node w/o publisher.
     */
    public JGroupsNode(LocalNode localNode, JChannel channel) throws IOException {
        super(JGroupsNodeConfig.NAME, 500);
        this.localNode = localNode;
        this.channel = channel;
        this.messageDispatcher = new MessageDispatcher(channel);
        this.tx = null;
        this.publisher = null;
    }

    /**
     * Creates JGroups node with publisher.
     */
    public JGroupsNode(LocalNode localNode, JChannel channel, Config config, PublisherFactory publisherFactory)
            throws IOException {
        super(JGroupsNodeConfig.NAME, 500);
        this.localNode = localNode;
        this.channel = channel;
        this.messageDispatcher = new MessageDispatcher(channel, this);
        this.tx = new ConcurrentHashMap<>();
        this.publisher = publisherFactory.createPublisher(config, txid -> {
            LocalEntry entry = tx.get(txid);
            if (entry == null) {
                return Optional.empty();
            }
            return Optional.of(entry);
        });
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
                    URI handle = URI.create(requireNonNull(data.get(PUBLISHER_HANDLE), PUBLISHER_HANDLE));
                    return Optional.of(localNode.store(key, new PublisherRemoteEntry(this, data, handle)));
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
            HashMap<String, String> map = new HashMap<>();
            Optional<LocalEntry> optionalEntry = localNode.locate(key);
            if (optionalEntry.isPresent()) {
                LocalEntry entry = optionalEntry.orElseThrow();
                String txid = UUID.randomUUID().toString();
                tx.put(txid, entry);
                URI handle = publisher.createHandle(txid);
                map.putAll(entry.metadata());
                map.put(PUBLISHER_HANDLE, handle.toASCIIString());
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
        if (publisher != null) {
            publisher.close();
        }
        messageDispatcher.close();
        channel.close();
    }
}
