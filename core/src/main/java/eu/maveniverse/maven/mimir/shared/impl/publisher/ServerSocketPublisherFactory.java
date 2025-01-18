/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.shared.impl.publisher;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.mimir.shared.Config;
import eu.maveniverse.maven.mimir.shared.node.LocalEntry;
import eu.maveniverse.maven.mimir.shared.publisher.Publisher;
import eu.maveniverse.maven.mimir.shared.publisher.PublisherFactory;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Optional;
import java.util.function.Function;
import javax.inject.Named;
import javax.inject.Singleton;

@Singleton
@Named(ServerSocketPublisherFactory.NAME)
public class ServerSocketPublisherFactory implements PublisherFactory {
    public static final String NAME = "socket";

    @Override
    public Publisher createPublisher(Config config, Function<String, Optional<LocalEntry>> entrySupplier)
            throws IOException {
        requireNonNull(config, "config");
        requireNonNull(entrySupplier, "entrySupplier");
        return new ServerSocketPublisher(new InetSocketAddress(0), entrySupplier);
    }
}
