/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.shared.node;

/**
 * A remote node represents some generic storage on some remote place.
 */
public interface RemoteNode<E extends RemoteEntry> extends Node<E> {
    /**
     * The "distance" of node. Distance of 0 (zero) is "closest", and positive integers are "further away".
     */
    int distance();
}
