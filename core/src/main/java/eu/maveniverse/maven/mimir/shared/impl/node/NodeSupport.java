/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.shared.impl.node;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.mimir.shared.node.Node;
import eu.maveniverse.maven.shared.core.component.CloseableSupport;

public abstract class NodeSupport extends CloseableSupport implements Node {
    protected final String name;

    public NodeSupport(String name) {
        this.name = requireNonNull(name, "name");
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public abstract String toString();
}
