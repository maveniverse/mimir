/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.node.daemon.protocol;

import static java.util.Objects.requireNonNull;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.msgpack.core.MessagePack;
import org.msgpack.core.MessagePacker;
import org.msgpack.core.MessageUnpacker;

public class Handle implements Closeable {
    private final DataOutputStream outputStream;
    private final DataInputStream inputStream;

    public Handle(OutputStream outputStream, InputStream inputStream) {
        this.outputStream = new DataOutputStream(requireNonNull(outputStream, "outputStream"));
        this.inputStream = new DataInputStream(requireNonNull(inputStream, "inputStream"));
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
        outputStream.close();
        inputStream.close();
    }

    private void packMap(MessagePacker p, Map<String, String> map) throws IOException {
        p.packMapHeader(map.size());
        for (Map.Entry<String, String> entry : map.entrySet()) {
            p.packString(entry.getKey());
            p.packString(entry.getValue());
        }
    }

    private Map<String, String> unpackMap(MessageUnpacker u) throws IOException {
        HashMap<String, String> metadata = new HashMap<>();
        int entries = u.unpackMapHeader();
        for (int i = 0; i < entries; i++) {
            String key = u.unpackString();
            String value = u.unpackString();
            metadata.put(key, value);
        }
        return metadata;
    }

    public static Map<String, String> listToMap(List<String> data) {
        requireNonNull(data, "data");
        AtomicInteger counter = new AtomicInteger(0);
        return data.stream().collect(Collectors.toMap(e -> String.valueOf(counter.incrementAndGet()), e -> e));
    }

    public static List<String> mapToList(Map<String, String> data) {
        requireNonNull(data, "data");
        ArrayList<String> result = new ArrayList<>(data.size());
        for (int i = 1; i < data.size(); i++) {
            String value = data.get(String.valueOf(i));
            if (value != null) {
                result.add(value);
            } else {
                throw new IllegalArgumentException("Invalid map entry; no key for  " + i + "; data: " + data);
            }
        }
        return result;
    }
}
