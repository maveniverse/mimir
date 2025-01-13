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
import java.util.Optional;
import javax.inject.Named;
import javax.inject.Singleton;

@Singleton
@Named("simple")
public class SimpleNameMapperFactory implements NameMapperFactory {
    @Override
    public Optional<NameMapper> createNameMapper(Config config) {
        requireNonNull(config, "config");
        return Optional.of(new SimpleNameMapper());
    }
}
