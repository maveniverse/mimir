/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */

/**
 * Mimir daemon protocol.
 * <p>
 * A simple request/response like protocol, where each request MUST have response. Currently, communication channel is
 * closed after each round trip (as UDS is used for transport and is "cheap"). This choice currently is due simplicity,
 * as DaemonNode is called from Resolver on multiple threads, and to avoid keeping a pool or alike for now. Later
 * this may change.
 * <p>
 * Request carries "command", "data" map and "session" map (except for very first HELLO command from client).
 * Response carries "status", "data" map and "session" map (always).
 * <p>
 * Client receives "session" map with first HELLO command response, and reuses same map for all the communication.
 * Client should not modify the "session" map, but may inspect its contents.
 */
package eu.maveniverse.maven.mimir.daemon.protocol;
