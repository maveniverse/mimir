/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.shared.publisher;

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;

public interface Publisher extends Closeable {
    /**
     * Crafts a publisher specific URI based on passed in token.
     */
    URI createHandle(String txid) throws IOException;
}
