/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.daemon;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.shared.core.component.CloseableSupport;
import java.io.Closeable;
import java.net.URI;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.inject.Named;
import javax.inject.Singleton;

@Named
@Singleton
public class DaemonSession extends CloseableSupport implements Closeable {
    private final ConcurrentMap<String, Set<URI>> keys = new ConcurrentHashMap<>();

    public String createSession() {
        String sessionId = UUID.randomUUID().toString();
        keys.put(sessionId, ConcurrentHashMap.newKeySet());
        return sessionId;
    }

    public boolean dropSession(String sessionId) {
        requireNonNull(sessionId);
        Set<URI> k = keys.get(sessionId);
        if (k != null) {
            logger.info("Dropping session {} w/ {} keys", sessionId, k.size());
        }
        return k != null;
    }

    public void markKeyInSession(String sessionId, URI key) {
        requireNonNull(sessionId);
        requireNonNull(key);
        keys.get(sessionId).add(key);
    }

    public Optional<Set<URI>> getKeysInSession(String sessionId) {
        requireNonNull(sessionId);
        return Optional.ofNullable(keys.get(sessionId));
    }
}
