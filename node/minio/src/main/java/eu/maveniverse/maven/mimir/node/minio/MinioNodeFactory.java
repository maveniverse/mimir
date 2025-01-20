/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.node.minio;

import eu.maveniverse.maven.mimir.shared.Config;
import eu.maveniverse.maven.mimir.shared.node.SystemNodeFactory;
import java.io.IOException;
import javax.inject.Named;
import javax.inject.Singleton;

@Singleton
@Named(MinioNodeConfig.NAME)
public class MinioNodeFactory implements SystemNodeFactory {
    @Override
    public MinioNode createNode(Config config) throws IOException {
        throw new UnsupportedOperationException("not implemented");
    }
}
