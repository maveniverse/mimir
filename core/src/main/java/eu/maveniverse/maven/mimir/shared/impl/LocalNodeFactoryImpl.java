/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.shared.impl;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.mimir.shared.Config;
import eu.maveniverse.maven.mimir.shared.node.LocalNode;
import eu.maveniverse.maven.mimir.shared.node.LocalNodeFactory;
import java.io.IOException;
import javax.inject.Named;
import javax.inject.Singleton;

@Singleton
@Named
public final class LocalNodeFactoryImpl implements LocalNodeFactory {
    @Override
    public LocalNode createLocalNode(Config config) throws IOException {
        requireNonNull(config, "config");
        return new LocalNodeImpl(LocalNodeConfig.with(config));
    }
}
