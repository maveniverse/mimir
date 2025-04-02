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

public abstract class Message {
    public abstract Map<String, String> data();

    public abstract Map<String, String> session();

    public String requireData(String key) {
        requireNonNull(key, "key");
        if (data().containsKey(key)) {
            return data().get(key);
        } else {
            throw new IllegalStateException(String.format("Key '%s' not found in data", key));
        }
    }

    public String requireSession(String key) {
        requireNonNull(key, "key");
        if (session().containsKey(key)) {
            return session().get(key);
        } else {
            throw new IllegalStateException(String.format("Key '%s' not found in session", key));
        }
    }
}
