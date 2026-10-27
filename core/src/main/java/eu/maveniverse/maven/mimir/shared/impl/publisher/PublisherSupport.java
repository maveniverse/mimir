/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.shared.impl.publisher;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.mimir.shared.node.Entry;
import eu.maveniverse.maven.mimir.shared.node.LocalEntry;
import eu.maveniverse.maven.mimir.shared.node.LocalNode;
import eu.maveniverse.maven.mimir.shared.publisher.Publisher;
import eu.maveniverse.maven.shared.core.component.CloseableSupport;
import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public abstract class PublisherSupport extends CloseableSupport implements Publisher {
    protected static final class HandleImpl implements Publisher.Handle {
        private final URI handle;
        private final LocalEntry localEntry;

        public HandleImpl(final URI handle, final LocalEntry localEntry) {
            this.handle = requireNonNull(handle);
            this.localEntry = requireNonNull(localEntry);
        }

        @Override
        public URI handle() {
            return handle;
        }

        @Override
        public LocalEntry publishedEntry() {
            return localEntry;
        }
    }

    protected final LocalNode localNode;
    protected final PublisherConfig publisherConfig;
    /**
     * Handles never fetched (ie. requester picked another member's handle) would accumulate forever; expire them.
     */
    private static final Duration HANDLE_TTL = Duration.ofMinutes(10);

    private record Published(Instant created, LocalEntry entry) {}

    protected final ConcurrentMap<String, Published> publishedEntries;

    protected PublisherSupport(LocalNode localNode, PublisherConfig publisherConfig) {
        this.localNode = requireNonNull(localNode);
        this.publisherConfig = requireNonNull(publisherConfig);
        this.publishedEntries = new ConcurrentHashMap<>();
    }

    @Override
    public Optional<Handle> createHandle(URI key) throws IOException {
        Optional<? extends Entry> entry = localNode.locate(key);
        if (entry.isPresent()) {
            String token = UUID.randomUUID().toString();
            URI publishHandle = createHandle(token);
            LocalEntry e = (LocalEntry) entry.orElseThrow();
            Instant now = Instant.now();
            publishedEntries.values().removeIf(p -> p.created().isBefore(now.minus(HANDLE_TTL)));
            publishedEntries.put(token, new Published(now, e));
            return Optional.of(new HandleImpl(publishHandle, e));
        }
        return Optional.empty();
    }

    protected Optional<LocalEntry> publishedEntry(String token) {
        return Optional.ofNullable(publishedEntries.remove(token)).map(Published::entry);
    }

    protected abstract URI createHandle(String token) throws IOException;
}
