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
import eu.maveniverse.maven.mimir.shared.node.SystemNode;
import eu.maveniverse.maven.mimir.shared.publisher.Publisher;
import eu.maveniverse.maven.mimir.shared.publisher.PublisherFactory;
import java.io.IOException;
import javax.inject.Named;
import javax.inject.Singleton;

@Singleton
@Named(HttpServerPublisherFactory.NAME)
public class HttpServerPublisherFactory implements PublisherFactory {
    public static final String NAME = "http";

    @Override
    public Publisher createPublisher(Config config, SystemNode<?> systemNode) throws IOException {
        requireNonNull(config, "config");
        requireNonNull(systemNode, "systemNode");
        return new HttpServerPublisher(systemNode, PublisherConfig.with(config));
    }
}
