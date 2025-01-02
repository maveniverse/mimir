/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.shared.impl;

import eu.maveniverse.maven.mimir.shared.node.LocalNode;
import eu.maveniverse.maven.mimir.shared.node.LocalNodeFactory;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import javax.inject.Named;
import javax.inject.Singleton;

@Singleton
@Named(LocalNodeFactoryImpl.NAME)
public final class LocalNodeFactoryImpl implements LocalNodeFactory {
    public static final String NAME = "local";

    @Override
    public LocalNode createLocalNode(Map<String, Object> config) throws IOException {
        Path localBaseDir =
                Paths.get((String) config.get("user.home")).resolve(".mimir").resolve("local");
        Files.createDirectories(localBaseDir);
        return new LocalNodeImpl(NAME, 0, localBaseDir);
    }
}
