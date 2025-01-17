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
import eu.maveniverse.maven.mimir.shared.naming.NameMapper;
import eu.maveniverse.maven.mimir.shared.naming.NameMapperFactory;
import javax.inject.Named;
import javax.inject.Singleton;

@Singleton
@Named(SimpleNameMapperFactory.NAME)
public final class SimpleNameMapperFactory implements NameMapperFactory {
    public static final String NAME = "simple";

    @Override
    public NameMapper createNameMapper(Config config) {
        requireNonNull(config, "config");
        return new SimpleNameMapper();
    }

    /**
     * This is SIMPLE name mapper; fully usable for any standard scenario.
     * More logic may be needed for more complex scenarios, like proper identification of remote repositories,
     * support repo aliases, mirrors, etc.
     * <p>
     * Note: the layout this name mapper uses is intentionally non-standard, and is selected on purpose: to discourage
     * any direct tampering with cache contents. In essence, same rules applies as are in effect for Maven local repository:
     * no direct tampering. The layout should be considered "internal" and may change without any compatibility obligation.
     */
    private static class SimpleNameMapper implements NameMapper {}
}
