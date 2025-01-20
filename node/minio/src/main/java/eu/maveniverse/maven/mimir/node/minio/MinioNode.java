/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.node.minio;

import eu.maveniverse.maven.mimir.shared.impl.NodeSupport;
import eu.maveniverse.maven.mimir.shared.node.Entry;
import eu.maveniverse.maven.mimir.shared.node.SystemEntry;
import eu.maveniverse.maven.mimir.shared.node.SystemNode;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.eclipse.aether.spi.connector.checksum.ChecksumAlgorithmFactory;

public final class MinioNode extends NodeSupport implements SystemNode {
    public MinioNode(String name) {
        super(name);
    }

    @Override
    public List<String> checksumAlgorithms() throws IOException {
        return List.of();
    }

    @Override
    public Map<String, ChecksumAlgorithmFactory> checksumFactories() throws IOException {
        return Map.of();
    }

    @Override
    public Optional<? extends SystemEntry> locate(URI key) throws IOException {
        return Optional.empty();
    }

    @Override
    public SystemEntry store(URI key, Entry entry) throws IOException {
        return null;
    }

    @Override
    public void store(URI key, Path file, Map<String, String> checksums) throws IOException {}

    @Override
    public String toString() {
        return "";
    }
}
