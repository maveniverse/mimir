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
import eu.maveniverse.maven.mimir.shared.naming.KeyResolver;
import eu.maveniverse.maven.mimir.shared.naming.KeyResolverFactory;
import javax.inject.Named;
import javax.inject.Singleton;

@Singleton
@Named(SimpleKeyResolverFactory.NAME)
public final class SimpleKeyResolverFactory implements KeyResolverFactory {
    public static final String NAME = "simple";

    @Override
    public KeyResolver createKeyResolver(Config config) {
        requireNonNull(config, "config");
        return new SimpleKeyResolver();
    }

    /**
     * This is SIMPLE key resolver; fully usable for any standard scenario.
     * <p>
     * Note: the layout this name mapper uses is intentionally non-standard, and is selected on purpose: to discourage
     * any direct tampering with cache contents. In essence, same rules applies as are in effect for Maven local repository:
     * no direct tampering. The layout should be considered "internal" and may change without any compatibility obligation.
     */
    private static class SimpleKeyResolver implements KeyResolver {}
}
