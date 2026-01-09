/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.node.ipfs;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.mimir.shared.impl.node.RemoteNodeSupport;
import eu.maveniverse.maven.mimir.shared.node.Entry;
import eu.maveniverse.maven.mimir.shared.node.LocalEntry;
import eu.maveniverse.maven.mimir.shared.node.RemoteNode;
import eu.maveniverse.maven.mimir.shared.node.SystemNode;
import io.ipfs.api.IPFS;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class IpfsNode extends RemoteNodeSupport implements SystemNode, RemoteNode {
    private final String multiaddr;
    private final IPFS ipfs;

    public IpfsNode(String multiaddr) {
        super(IpfsNodeConfig.NAME, 5000);
        this.multiaddr = requireNonNull(multiaddr);
        this.ipfs = new IPFS(multiaddr);
    }

    @Override
    public List<String> checksumAlgorithms() throws IOException {
        return List.of();
    }

    @Override
    public Optional<IpfsEntry> locate(URI key) throws IOException {
        return Optional.empty();
    }

    @Override
    public LocalEntry store(URI key, Path file, Map<String, String> metadata, Map<String, String> checksums)
            throws IOException {
        return null;
    }

    @Override
    public LocalEntry store(URI key, Entry entry) throws IOException {
        return null;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " (distance=" + distance + " multiaddr=" + multiaddr + ")";
    }

    @Override
    protected void doClose() {}
}
