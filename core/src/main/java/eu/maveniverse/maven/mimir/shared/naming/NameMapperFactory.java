/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.shared.naming;

import eu.maveniverse.maven.mimir.shared.Config;
import java.io.IOException;

public interface NameMapperFactory {
    /**
     * Creates a {@link NameMapper} instance, if supported.
     */
    NameMapper createNameMapper(Config config) throws IOException;
}
