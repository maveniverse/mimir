/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.daemon.protocol;

import static java.util.Objects.requireNonNull;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.channels.ByteChannel;
import java.nio.channels.Channels;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.msgpack.core.MessagePack;
import org.msgpack.core.MessagePacker;
import org.msgpack.core.MessageUnpacker;

public class Handle implements Closeable {
    /**
     * The default unix socket path (resolved from Mimir basedir).
     */
    public static final String DEFAULT_SOCKET_PATH = "mimir-socket";

    private final ByteChannel channel;
    private final DataOutputStream outputStream;
    private final DataInputStream inputStream;

    public interface ServerHandle extends Closeable {
        boolean isOpen();

        Handle accept() throws IOException;
    }

    public static ServerHandle serverDomainSocket(Path domainSocketPath) throws IOException {
        requireNonNull(domainSocketPath, "domainSocketPath");
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open(StandardProtocolFamily.UNIX);
        serverSocketChannel.configureBlocking(true);
        serverSocketChannel.bind(UnixDomainSocketAddress.of(domainSocketPath));
        return new ServerHandle() {
            private final AtomicBoolean closed = new AtomicBoolean(false);

            @Override
            public boolean isOpen() {
                return serverSocketChannel.isOpen();
            }

            @Override
            public Handle accept() throws IOException {
                return new Handle(serverSocketChannel.accept());
            }

            @Override
            public void close() throws IOException {
                if (closed.compareAndSet(false, true)) {
                    serverSocketChannel.close();
                }
            }
        };
    }

    public interface ClientHandle extends Closeable {
        boolean isOpen();

        Handle getHandle() throws IOException;
    }

    public static ClientHandle clientDomainSocket(Path domainSocketPath) throws IOException {
        requireNonNull(domainSocketPath, "domainSocketPath");
        return new ClientHandle() {
            private final AtomicBoolean closed = new AtomicBoolean(false);

            @Override
            public boolean isOpen() {
                return !closed.get();
            }

            @Override
            public Handle getHandle() throws IOException {
                if (closed.get()) {
                    throw new IllegalStateException("ClientHandle has been closed");
                }
                SocketChannel socketChannel = SocketChannel.open(StandardProtocolFamily.UNIX);
                socketChannel.configureBlocking(true);
                socketChannel.connect(UnixDomainSocketAddress.of(domainSocketPath));
                return new Handle(socketChannel);
            }

            @Override
            public void close() {
                if (closed.compareAndSet(false, true)) {
                    // nothing
                }
            }
        };
    }

    /**
     * This method is used in tests.
     */
    public static Handle byteChannel(ByteChannel byteChannel) {
        return new Handle(byteChannel);
    }

    private Handle(ByteChannel byteChannel) {
        this.channel = requireNonNull(byteChannel, "byteChannel");
        this.outputStream = new DataOutputStream(Channels.newOutputStream(channel));
        this.inputStream = new DataInputStream(Channels.newInputStream(channel));
    }

    public void writeRequest(Request request) throws IOException {
        requireNonNull(request, "request");
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        try (MessagePacker p = MessagePack.newDefaultPacker(b)) {
            p.packString(request.cmd());
            packMap(p, request.data());
            packMap(p, request.session());
        }
        outputStream.writeInt(b.size());
        outputStream.write(b.toByteArray());
        outputStream.flush();
    }

    public Request readRequest() throws IOException {
        byte[] bytes = inputStream.readNBytes(inputStream.readInt());
        try (MessageUnpacker u = MessagePack.newDefaultUnpacker(bytes)) {
            return ImmutableRequest.builder()
                    .cmd(u.unpackString())
                    .data(unpackMap(u))
                    .session(unpackMap(u))
                    .build();
        }
    }

    public void writeResponse(Response response) throws IOException {
        requireNonNull(response, "response");
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        try (MessagePacker p = MessagePack.newDefaultPacker(b)) {
            p.packString(response.status());
            packMap(p, response.data());
            packMap(p, response.session());
        }
        outputStream.writeInt(b.size());
        outputStream.write(b.toByteArray());
        outputStream.flush();
    }

    public Response readResponse() throws IOException {
        byte[] bytes = inputStream.readNBytes(inputStream.readInt());
        try (MessageUnpacker u = MessagePack.newDefaultUnpacker(bytes)) {
            return ImmutableResponse.builder()
                    .status(u.unpackString())
                    .data(unpackMap(u))
                    .session(unpackMap(u))
                    .build();
        }
    }

    @Override
    public void close() throws IOException {
        channel.close();
    }

    private void packMap(MessagePacker p, Map<String, String> map) throws IOException {
        p.packMapHeader(map.size());
        for (Map.Entry<String, String> entry : map.entrySet()) {
            p.packString(entry.getKey());
            p.packString(entry.getValue());
        }
    }

    private Map<String, String> unpackMap(MessageUnpacker u) throws IOException {
        LinkedHashMap<String, String> metadata = new LinkedHashMap<>();
        int entries = u.unpackMapHeader();
        for (int i = 0; i < entries; i++) {
            String key = u.unpackString();
            String value = u.unpackString();
            metadata.put(key, value);
        }
        return metadata;
    }
}
