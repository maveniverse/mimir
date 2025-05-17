/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.shared.impl.publisher;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.mimir.shared.node.SystemEntry;
import eu.maveniverse.maven.mimir.shared.node.SystemNode;
import eu.maveniverse.maven.mimir.shared.publisher.Publisher;
import eu.maveniverse.maven.shared.core.component.CloseableSupport;
import java.io.IOException;
import java.net.URI;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public abstract class PublisherSupport extends CloseableSupport implements Publisher {
    protected static final class HandleImpl implements Publisher.Handle {
        private final URI handle;
        private final SystemEntry systemEntry;

        public HandleImpl(final URI handle, final SystemEntry systemEntry) {
            this.handle = requireNonNull(handle);
            this.systemEntry = requireNonNull(systemEntry);
        }

        @Override
        public URI handle() {
            return handle;
        }

        @Override
        public SystemEntry publishedEntry() {
            return systemEntry;
        }
    }

    protected final SystemNode<?> systemNode;
    protected final PublisherConfig publisherConfig;
    protected final ConcurrentMap<String, SystemEntry> publishedEntries;

    protected PublisherSupport(SystemNode<?> systemNode, PublisherConfig publisherConfig) {
        this.systemNode = requireNonNull(systemNode);
        this.publisherConfig = requireNonNull(publisherConfig);
        this.publishedEntries = new ConcurrentHashMap<>();
    }

    @Override
    public Optional<Handle> createHandle(URI key) throws IOException {
        Optional<? extends SystemEntry> entry = systemNode.locate(key);
        if (entry.isPresent()) {
            String token = UUID.randomUUID().toString();
            URI publishHandle = createHandle(token);
            SystemEntry systemEntry = entry.orElseThrow();
            publishedEntries.put(token, systemEntry);
            return Optional.of(new HandleImpl(publishHandle, systemEntry));
        }
        return Optional.empty();
    }

    protected Optional<SystemEntry> publishedEntry(String token) {
        return Optional.ofNullable(publishedEntries.remove(token));
    }

    protected abstract URI createHandle(String token) throws IOException;
}
