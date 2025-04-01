/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.node.daemon.protocol;

import static java.util.Objects.requireNonNull;

import com.fasterxml.jackson.core.JsonParser;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.msgpack.jackson.dataformat.MessagePackMapper;

public class Handle implements Closeable {
    private final MessagePackMapper mapper = new MessagePackMapper();
    private final OutputStream outputStream;
    private final InputStream inputStream;

    public Handle(OutputStream outputStream, InputStream inputStream) {
        this.outputStream = new BufferedOutputStream(requireNonNull(outputStream));
        this.inputStream = new BufferedInputStream(requireNonNull(inputStream));

        this.mapper.configure(JsonParser.Feature.AUTO_CLOSE_SOURCE, false);
    }

    public void writeRequest(Request request) throws IOException {
        requireNonNull(request, "request");
        outputStream.write(mapper.writeValueAsBytes(request));
        outputStream.flush();
    }

    public Request readRequest() throws IOException {
        return mapper.readValue(inputStream, Request.class);
    }

    public void writeResponse(Response response) throws IOException {
        requireNonNull(response);
        outputStream.write(mapper.writeValueAsBytes(response));
        outputStream.flush();
    }

    public Response readResponse() throws IOException {
        Response response = mapper.readValue(inputStream, Response.class);
        if (!Response.STATUS_OK.equals(response.status())) {
            throw new IOException(response.data().get(Response.DATA_MESSAGE));
        }
        return response;
    }

    @Override
    public void close() throws IOException {
        outputStream.close();
        inputStream.close();
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
