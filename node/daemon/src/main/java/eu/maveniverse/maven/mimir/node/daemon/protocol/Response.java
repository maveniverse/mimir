/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.node.daemon.protocol;

import static java.util.Objects.requireNonNull;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.util.Map;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableResponse.class)
@JsonDeserialize(as = ImmutableResponse.class)
public abstract class Response extends Message {
    public static final String STATUS_OK = "OK";
    public static final String STATUS_KO = "KO ";

    public static final String DATA_MESSAGE = "message";

    public abstract String status();

    public static Response okMessage(Request request, String message) {
        requireNonNull(message, "message");
        return okData(request, Map.of(DATA_MESSAGE, message));
    }

    public static Response okData(Request request, Map<String, String> data) {
        requireNonNull(data, "data");
        return ImmutableResponse.builder()
                .status(Response.STATUS_OK)
                .data(data)
                .session(request.session())
                .build();
    }

    public static Response koMessage(Request request, String message) {
        requireNonNull(message, "message");
        return ImmutableResponse.builder()
                .status(Response.STATUS_KO)
                .data(Map.of(DATA_MESSAGE, message))
                .session(request.session())
                .build();
    }
}
