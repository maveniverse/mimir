/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.node.jgroups;

import static eu.maveniverse.maven.mimir.shared.impl.Utils.mergeEntry;
import static eu.maveniverse.maven.mimir.shared.impl.Utils.splitChecksums;
import static eu.maveniverse.maven.mimir.shared.impl.Utils.splitMetadata;
import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.mimir.shared.impl.node.RemoteNodeSupport;
import eu.maveniverse.maven.mimir.shared.impl.publisher.PublisherRemoteEntry;
import eu.maveniverse.maven.mimir.shared.node.Entry;
import eu.maveniverse.maven.mimir.shared.publisher.Publisher;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import org.jgroups.Address;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.ObjectMessage;
import org.jgroups.Receiver;
import org.jgroups.View;
import org.jgroups.blocks.MessageDispatcher;
import org.jgroups.blocks.RequestHandler;
import org.jgroups.blocks.RequestOptions;
import org.jgroups.blocks.Response;
import org.jgroups.util.RspList;

public class JGroupsNode extends RemoteNodeSupport<PublisherRemoteEntry> implements Receiver, RequestHandler {
    private static final String PUBLISHER_HANDLE = "handle";
    private static final String CMD_LOCATE = "locate";
    private static final String RSP_ERROR = "error";

    private final JChannel channel;
    private final MessageDispatcher messageDispatcher;
    private final Publisher publisher;
    private final AtomicReference<View> lastView;
    private final ExecutorService executor;

    /**
     * Creates JGroups node w/o publisher.
     */
    public JGroupsNode(String clusterName, JChannel channel) throws Exception {
        super(JGroupsNodeConfig.NAME, 500);
        this.channel = channel;
        this.messageDispatcher = new MessageDispatcher(channel);
        this.messageDispatcher.setAsynDispatching(true);
        this.messageDispatcher.setReceiver(this);
        this.publisher = null;
        this.lastView = new AtomicReference<>(null);
        // Java 21: this.executor = Executors.newVirtualThreadPerTaskExecutor();
        this.executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() - 1);

        channel.connect(clusterName, null, 1500);
    }

    /**
     * Creates JGroups node with publisher.
     */
    public JGroupsNode(String clusterName, JChannel channel, Publisher publisher) throws Exception {
        super(JGroupsNodeConfig.NAME, 500);
        this.channel = channel;
        this.messageDispatcher = new MessageDispatcher(channel, this);
        this.messageDispatcher.setAsynDispatching(true);
        this.messageDispatcher.setReceiver(this);
        this.publisher = publisher;
        this.lastView = new AtomicReference<>(null);
        // Java 21: this.executor = Executors.newVirtualThreadPerTaskExecutor();
        this.executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() - 1);

        channel.connect(clusterName, null, 1500);
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
                    if (data.containsKey(PUBLISHER_HANDLE)) {
                        URI handle = URI.create(requireNonNull(data.remove(PUBLISHER_HANDLE), PUBLISHER_HANDLE));
                        return Optional.of(new PublisherRemoteEntry(splitMetadata(data), splitChecksums(data), handle));
                    } else {
                        throw new IOException(data.remove(RSP_ERROR));
                    }
                }
            }
        } catch (Exception e) {
            throw new IOException("Failed to locate", e);
        }
        return Optional.empty();
    }

    @Override
    public void viewAccepted(View view) {
        View prev = lastView.get();
        logger.info("Cluster {}: ", prev == null ? "info" : "update");
        logger.info("  Members: {}", view.getMembers());
        if (prev != null) {
            List<Address> newMembers = View.newMembers(prev, view);
            if (!newMembers.isEmpty()) {
                logger.info("  New members: {}", newMembers);
            }

            List<Address> leftMembers = View.leftMembers(prev, view);
            if (!leftMembers.isEmpty()) {
                logger.info("  Left members: {}", leftMembers);
            }
        }
        lastView.compareAndSet(prev, view);
    }

    @Override
    public Object handle(Message msg) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void handle(Message msg, Response response) {
        executor.submit(() -> {
            Thread.currentThread().setName("JVT");
            HashMap<String, String> responseMap = new HashMap<>();
            boolean responseException = false;
            try {
                List<String> req = msg.getObject();
                if (req.size() == 2 && CMD_LOCATE.equals(req.get(0))) {
                    String keyString = req.get(1);
                    URI key = URI.create(keyString);
                    Optional<Publisher.Handle> handle = publisher.createHandle(key);
                    if (handle.isPresent()) {
                        Publisher.Handle publisherHandle = handle.orElseThrow();
                        Entry publishedEntry = publisherHandle.publishedEntry();
                        URI publishedHandle = publisherHandle.handle();
                        responseMap.putAll(mergeEntry(publishedEntry));
                        responseMap.put(PUBLISHER_HANDLE, publishedHandle.toASCIIString());
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
        return getClass().getSimpleName() + " (distance=" + distance + " channel=" + channel.getAddress()
                + " clusterName=" + channel.getClusterName() + " publisher=" + publisher + ")";
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
