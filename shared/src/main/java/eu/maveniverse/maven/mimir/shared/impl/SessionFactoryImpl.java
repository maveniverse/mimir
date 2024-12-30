/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.shared.impl;

import eu.maveniverse.maven.mimir.shared.Session;
import eu.maveniverse.maven.mimir.shared.SessionFactory;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map;
import javax.inject.Named;
import javax.inject.Singleton;

@Singleton
@Named
public class SessionFactoryImpl implements SessionFactory {
    @Override
    public Session createSession(Map<String, Object> config) throws IOException {
        Path localBaseDir =
                Paths.get(System.getProperty("user.home")).resolve(".mimir").resolve("local");
        Files.createDirectories(localBaseDir);
        LocalNodeImpl localNode = new LocalNodeImpl(localBaseDir);
        return new SessionImpl(Collections.singletonList(localNode));
    }
}
