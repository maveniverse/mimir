/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.shared.impl.publisher;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.mimir.shared.impl.node.EntrySupport;
import eu.maveniverse.maven.mimir.shared.node.RemoteEntry;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class PublisherRemoteEntry extends EntrySupport implements RemoteEntry {
    private final URI handle;

    public PublisherRemoteEntry(Map<String, String> metadata, Map<String, String> checksums, URI handle) {
        super(metadata, checksums);
        this.handle = requireNonNull(handle);
    }

    @Override
    public void handleContent(IOConsumer consumer) throws IOException {
        requireNonNull(consumer);
        String schema = handle.getScheme();
        try {
            if ("http".equals(schema)) {
                try (InputStream inputStream = handle.toURL().openConnection().getInputStream()) {
                    consumer.accept(inputStream);
                }
            } else if ("socket".equals(schema)) {
                try (Socket socket = new Socket(handle.getHost(), handle.getPort())) {
                    OutputStream os = socket.getOutputStream();
                    os.write(handle.getPath().substring(1).getBytes(StandardCharsets.UTF_8));
                    os.flush();
                    consumer.accept(socket.getInputStream());
                }
            } else {
                throw new IOException("Unknown protocol: " + schema);
            }
        } catch (IOException e) {
            throw new IOException("Failed to get artifact content from publisher at " + handle.toASCIIString(), e);
        }
    }
}
