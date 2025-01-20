/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.shared.impl;

import eu.maveniverse.maven.mimir.shared.node.RemoteEntry;
import java.util.Map;

public abstract class RemoteEntrySupport extends EntrySupport implements RemoteEntry {
    public RemoteEntrySupport(Map<String, String> metadata, Map<String, String> checksums) {
        super(metadata, checksums);
    }
}
