/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.shared.impl.node;

import eu.maveniverse.maven.mimir.shared.node.Entry;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class EntrySupport implements Entry {
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    protected final Map<String, String> metadata;
    protected final Map<String, String> checksums;

    public EntrySupport(Map<String, String> metadata, Map<String, String> checksums) {
        this.metadata = Map.copyOf(metadata);
        this.checksums = Map.copyOf(checksums);
    }

    @Override
    public Map<String, String> metadata() {
        return metadata;
    }

    @Override
    public Map<String, String> checksums() {
        return checksums;
    }
}
