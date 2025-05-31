/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.daemon.protocol;

public final class Session {
    private Session() {}

    // daemon
    public static final String DAEMON_PID = "daemon.pid";
    public static final String DAEMON_VERSION = "daemon.version";

    // node
    public static final String NODE_PID = "node.pid";
    public static final String NODE_VERSION = "node.version";

    // session
    public static final String SESSION_ID = "sessionId";

    // lrm
    public static final String LRM_PREFIX = "lrm.";
    public static final String LRM_PATH = LRM_PREFIX + "path";
}
