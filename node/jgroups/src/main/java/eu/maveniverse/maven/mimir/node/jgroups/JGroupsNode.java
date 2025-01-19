/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.node.jgroups;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.mimir.shared.Config;
import eu.maveniverse.maven.mimir.shared.impl.NodeSupport;
import eu.maveniverse.maven.mimir.shared.impl.publisher.PublisherRemoteEntry;
import eu.maveniverse.maven.mimir.shared.node.RemoteNode;
import eu.maveniverse.maven.mimir.shared.node.SystemEntry;
import eu.maveniverse.maven.mimir.shared.node.SystemNode;
import eu.maveniverse.maven.mimir.shared.publisher.Publisher;
import eu.maveniverse.maven.mimir.shared.publisher.PublisherFactory;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.jgroups.Address;
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

public class JGroupsNode extends NodeSupport implements RemoteNode, RequestHandler {
    private static final String PUBLISHER_HANDLE = "handle";
    private static final String CMD_LOCATE = "locate";
    private static final String RSP_ERROR = "error";

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final SystemNode systemNode;
    private final JChannel channel;
    private final MessageDispatcher messageDispatcher;
    private final Publisher publisher;
    private final ConcurrentHashMap<String, SystemEntry> tx;

    /**
     * Creates JGroups node w/o publisher.
     */
    public JGroupsNode(SystemNode systemNode, JChannel channel) {
        super(JGroupsNodeConfig.NAME, 500);
        this.systemNode = systemNode;
        this.channel = channel;
        this.messageDispatcher = new MessageDispatcher(channel);
        this.messageDispatcher.setAsynDispatching(true);
        this.tx = null;
        this.publisher = null;
    }

    /**
     * Creates JGroups node with publisher.
     */
    public JGroupsNode(SystemNode systemNode, JChannel channel, Config config, PublisherFactory publisherFactory)
            throws IOException {
        super(JGroupsNodeConfig.NAME, 500);
        this.systemNode = systemNode;
        this.channel = channel;
        this.messageDispatcher = new MessageDispatcher(channel, this);
        this.messageDispatcher.setAsynDispatching(true);
        this.tx = new ConcurrentHashMap<>();
        this.publisher = publisherFactory.createPublisher(config, txid -> {
            SystemEntry entry = tx.get(txid);
            if (entry == null) {
                return Optional.empty();
            }
            return Optional.of(entry);
        });
    }

    @Override
    public Optional<PublisherRemoteEntry> locate(URI key) throws IOException {
        ArrayList<String> req = new ArrayList<>();
        req.add(CMD_LOCATE);
        req.add(key.toASCIIString());
        try {
            RspList<Map<String, String>> responses =
                    messageDispatcher.castMessage(null, new ObjectMessage(null, req), RequestOptions.SYNC());
            for (Address responder : responses.keySet()) {
                Map<String, String> data = responses.get(responder).getValue();
                if (!data.isEmpty()) {
                    URI handle = URI.create(requireNonNull(data.remove(PUBLISHER_HANDLE), PUBLISHER_HANDLE));
                    return Optional.of(new PublisherRemoteEntry(data, handle));
                }
            }
        } catch (Exception e) {
            throw new IOException("Failed to locate", e);
        }
        return Optional.empty();
    }

    @Override
    public Object handle(Message msg) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void handle(Message msg, Response response) {
        Thread.startVirtualThread(() -> {
            HashMap<String, String> responseMap = new HashMap<>();
            boolean responseException = false;
            try {
                List<String> req = msg.getObject();
                if (req.size() == 2 && CMD_LOCATE.equals(req.getFirst())) {
                    String keyString = req.get(1);
                    URI key = URI.create(keyString);
                    Optional<? extends SystemEntry> optionalEntry = systemNode.locate(key);
                    if (optionalEntry.isPresent()) {
                        SystemEntry entry = optionalEntry.orElseThrow();
                        String txid = UUID.randomUUID().toString();
                        tx.put(txid, entry);
                        URI handle = publisher.createHandle(txid);
                        responseMap.putAll(entry.metadata());
                        responseMap.put(PUBLISHER_HANDLE, handle.toASCIIString());
                        logger.info("OK: {} asked {}", msg.getSrc(), keyString);
                    } else {
                        logger.info("KO: {} asked {}", msg.getSrc(), keyString);
                    }
                } else {
                    responseMap.put(RSP_ERROR, "Unknown command");
                    responseException = true;
                    logger.info("UNKNOWN COMMAND: {}", req);
                }
                response.send(responseMap, responseException);
            } catch (IOException e) {
                responseMap.put(RSP_ERROR, e.getMessage());
                response.send(responseMap, true);
            }
        });
    }

    @Override
    public String toString() {
        return "jgroups (distance=" + distance + " channel=" + channel.getAddress() + " publisher=" + publisher + ")";
    }

    @Override
    protected void doClose() throws IOException {
        if (publisher != null) {
            publisher.close();
        }
        messageDispatcher.close();
        channel.close();
    }
}
