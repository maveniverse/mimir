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
import java.util.HashMap;
import java.util.Map;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableRequest.class)
@JsonDeserialize(as = ImmutableRequest.class)
public abstract class Request extends Message {
    public static final String CMD_HELLO = "HELLO";
    public static final String CMD_BYE = "BYE";
    public static final String CMD_LOCATE = "LOCATE";
    public static final String CMD_TRANSFER = "TRANSFER";
    public static final String CMD_LS_CHECKSUMS = "LS_CHECKSUMS";
    public static final String CMD_STORE_PATH = "STORE_PATH";

    public static final String DATA_KEYSTRING = "keyString";
    public static final String DATA_PATHSTRING = "pathString";
    public static final String DATA_SHUTDOWN = "shutdown";

    public static final String SESSION_ID = "sessionId";

    public abstract String cmd();

    public static Request hello(Map<String, String> data) {
        requireNonNull(data, "data");
        return ImmutableRequest.builder()
                .cmd(CMD_HELLO)
                .data(data)
                .session(Map.of())
                .build();
    }

    public static Request bye(Map<String, String> session, boolean shutdown) {
        requireNonNull(session, "session");
        Map<String, String> data = new HashMap<>();
        if (shutdown) {
            data.put(DATA_SHUTDOWN, Boolean.TRUE.toString());
        }
        return ImmutableRequest.builder()
                .cmd(CMD_BYE)
                .data(data)
                .session(session)
                .build();
    }

    public static Request locate(Map<String, String> session, String keyString) {
        requireNonNull(session, "session");
        requireNonNull(keyString, "keyString");
        return ImmutableRequest.builder()
                .cmd(CMD_LOCATE)
                .data(Map.of(DATA_KEYSTRING, keyString))
                .session(session)
                .build();
    }

    public static Request lsChecksums(Map<String, String> session) {
        requireNonNull(session, "session");
        return ImmutableRequest.builder()
                .cmd(CMD_LS_CHECKSUMS)
                .data(Map.of())
                .session(session)
                .build();
    }

    public static Request transfer(Map<String, String> session, String keyString, String filePath) {
        requireNonNull(session, "session");
        requireNonNull(keyString, "keyString");
        requireNonNull(filePath, "filePath");
        HashMap<String, String> requestData = new HashMap<>();
        requestData.put(DATA_KEYSTRING, keyString);
        requestData.put(DATA_PATHSTRING, filePath);
        return ImmutableRequest.builder()
                .cmd(CMD_TRANSFER)
                .data(requestData)
                .session(session)
                .build();
    }

    public static Request storePath(
            Map<String, String> session, String keyString, String filePath, Map<String, String> data) {
        requireNonNull(session, "session");
        requireNonNull(keyString, "keyString");
        requireNonNull(filePath, "filePath");
        requireNonNull(data, "data");
        HashMap<String, String> requestData = new HashMap<>(data);
        requestData.put(DATA_KEYSTRING, keyString);
        requestData.put(DATA_PATHSTRING, filePath);
        return ImmutableRequest.builder()
                .cmd(CMD_STORE_PATH)
                .data(requestData)
                .session(session)
                .build();
    }
}
