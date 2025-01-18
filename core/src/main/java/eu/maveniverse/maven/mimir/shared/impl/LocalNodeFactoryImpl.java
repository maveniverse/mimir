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
import eu.maveniverse.maven.mimir.shared.naming.KeyResolverFactory;
import eu.maveniverse.maven.mimir.shared.node.LocalNode;
import eu.maveniverse.maven.mimir.shared.node.LocalNodeFactory;
import java.io.IOException;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

@Singleton
@Named
public final class LocalNodeFactoryImpl implements LocalNodeFactory {
    private final Map<String, KeyResolverFactory> keyResolverFactories;

    @Inject
    public LocalNodeFactoryImpl(Map<String, KeyResolverFactory> keyResolverFactories) {
        this.keyResolverFactories = requireNonNull(keyResolverFactories, "keyResolverFactories");
    }

    @Override
    public LocalNode createLocalNode(Config config) throws IOException {
        requireNonNull(config, "config");
        LocalNodeConfig localNodeConfig = LocalNodeConfig.with(config);
        KeyResolverFactory keyResolverFactory = keyResolverFactories.get(localNodeConfig.keyResolver());
        if (keyResolverFactory == null) {
            throw new IllegalArgumentException("Unknown keyResolver: " + localNodeConfig.keyResolver());
        }
        return new LocalNodeImpl(localNodeConfig, keyResolverFactory.createKeyResolver(config));
    }
}
