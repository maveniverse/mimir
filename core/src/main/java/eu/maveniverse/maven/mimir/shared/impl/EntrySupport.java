/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.shared.impl;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.mimir.shared.node.Entry;
import eu.maveniverse.maven.mimir.shared.node.Node;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class EntrySupport implements Entry {
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    protected final Node origin;
    protected final Map<String, String> metadata;

    public EntrySupport(Node origin, Map<String, String> metadata) {
        this.origin = requireNonNull(origin, "origin");
        this.metadata = Map.copyOf(metadata);
    }

    @Override
    public Node origin() {
        return origin;
    }

    @Override
    public Map<String, String> metadata() {
        return metadata;
    }
}
