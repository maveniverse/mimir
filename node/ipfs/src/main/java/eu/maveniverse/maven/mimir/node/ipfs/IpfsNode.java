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
import eu.maveniverse.maven.mimir.shared.node.RemoteNode;
import io.ipfs.api.IPFS;
import io.ipfs.multihash.Multihash;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.eclipse.aether.spi.connector.checksum.ChecksumAlgorithmFactory;

public class IpfsNode extends RemoteNodeSupport implements RemoteNode {
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
    public Optional<IpfsEntry> locate(URI uri) throws IOException {
        Keys.Key key = UriDecoders.apply(uri);
        // We accept ArtifactKey or CasKey
        // IF artifact key we do GAV -> SHA1 -> CID
        // If CasKey and is sha1, we do SHA1 -> CID
        // If CasKey and is ipfs, we take it as CID
        Keys.CasKey sha1 = null;
        Keys.CasKey cid = null;
        if (key instanceof Keys.ArtifactKey artifactKey) {
            sha1 = null; // lookup
        }
        if (sha1 != null) {
            cid = null;
        }

        // gav -> sha1 -> cid
        if (key instanceof Keys.CasKey casKey) {
            if ("ipfs".equals(casKey.type())) {
                Multihash multihash = Multihash.decode(casKey.address());
            } else if ("sha1".equals(casKey.type())) {
            }
        }
        return Optional.empty();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " (distance=" + distance + " multiaddr=" + multiaddr + ")";
    }

    @Override
    protected void doClose() {}
}
