/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.shared.impl.publisher;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.mimir.shared.impl.RemoteEntrySupport;
import eu.maveniverse.maven.mimir.shared.node.Node;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class PublisherRemoteEntry extends RemoteEntrySupport {
    private final URI handle;

    public PublisherRemoteEntry(Node origin, Map<String, String> metadata, URI handle) {
        super(origin, metadata);
        this.handle = requireNonNull(handle, "handle");
    }

    @Override
    protected void handleContent(IOConsumer consumer) throws IOException {
        String schema = handle.getScheme();
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
    }
}
