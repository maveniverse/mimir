/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.daemon.protocol;

import static java.util.Objects.requireNonNull;

import java.util.Map;
import org.immutables.value.Value;

@Value.Immutable
public abstract class Response extends Message {
    public static final String STATUS_OK = "OK";
    public static final String STATUS_KO = "KO ";

    public static final String DATA_MESSAGE = "message";

    public abstract String status();

    public static Response okMessage(Request request, String message) {
        requireNonNull(request, "request");
        requireNonNull(message, "message");
        return response(request, STATUS_OK, Map.of(DATA_MESSAGE, message));
    }

    public static Response okData(Request request, Map<String, String> data) {
        requireNonNull(request, "request");
        requireNonNull(data, "data");
        return response(request, STATUS_OK, data);
    }

    public static Response koMessage(Request request, String message) {
        requireNonNull(request, "request");
        requireNonNull(message, "message");
        return response(request, STATUS_KO, Map.of(DATA_MESSAGE, message));
    }

    private static Response response(Request request, String status, Map<String, String> data) {
        return ImmutableResponse.builder()
                .status(status)
                .data(data)
                .session(request.session())
                .build();
    }
}
