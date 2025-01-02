/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.shared;

import java.io.IOException;
import java.util.Map;

public interface SessionFactory {
    /**
     * Creates a session. Session once unused should be closed.
     */
    Session createSession(Map<String, Object> config) throws IOException;
}
