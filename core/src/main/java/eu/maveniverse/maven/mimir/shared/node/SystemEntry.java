/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.shared.node;

import java.io.IOException;
import java.io.InputStream;

/**
 * System entry is a local entry that also supports publishing.
 */
public interface SystemEntry extends LocalEntry {
    /**
     * Provides entry content as stream.
     */
    InputStream inputStream() throws IOException;
}
