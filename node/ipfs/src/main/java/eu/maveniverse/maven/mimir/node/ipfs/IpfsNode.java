/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.node.ipfs;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.mimir.node.ipfs.lookup.ChecksumIndex;
import eu.maveniverse.maven.mimir.node.ipfs.lookup.ChecksumLookup;
import eu.maveniverse.maven.mimir.shared.impl.node.RemoteNodeSupport;
import eu.maveniverse.maven.mimir.shared.naming.Keys;
import eu.maveniverse.maven.mimir.shared.naming.UriDecoders;
import eu.maveniverse.maven.mimir.shared.node.Entry;
import eu.maveniverse.maven.mimir.shared.node.LocalEntry;
import eu.maveniverse.maven.mimir.shared.node.RemoteNode;
import eu.maveniverse.maven.mimir.shared.node.SystemNode;
import io.ipfs.api.IPFS;
import io.ipfs.multihash.Multihash;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.eclipse.aether.spi.connector.checksum.ChecksumAlgorithmFactory;

public class IpfsNode extends RemoteNodeSupport implements SystemNode, RemoteNode {
    private final String multiaddr;
    private final List<String> checksumAlgorithms;
    private final Map<String, ChecksumAlgorithmFactory> checksumFactories;
    private final IPFS ipfs;
    private final ChecksumLookup checksumLookup;
    private final ChecksumIndex checksumIndex;

    public IpfsNode(
            String multiaddr,
            List<String> checksumAlgorithms,
            Map<String, ChecksumAlgorithmFactory> checksumFactories) {
        super(IpfsNodeConfig.NAME, 5000);
        this.multiaddr = requireNonNull(multiaddr);
        this.checksumAlgorithms = requireNonNull(checksumAlgorithms);
        this.checksumFactories = requireNonNull(checksumFactories);

        this.ipfs = new IPFS(multiaddr);
        this.checksumLookup = null;
        this.checksumIndex = null;
    }

    @Override
    public List<String> checksumAlgorithms() {
        return checksumAlgorithms;
    }

    @Override
    public Optional<IpfsEntry> locate(URI uri) throws IOException {
        Keys.Key key = UriDecoders.apply(uri);
        if (key instanceof Keys.CasKey casKey) {
            if ("ipfs".equals(casKey.type())) {
                Multihash multihash = Multihash.decode(casKey.address());
            } else if ("sha1".equals(casKey.type())) {
            }
        }
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
