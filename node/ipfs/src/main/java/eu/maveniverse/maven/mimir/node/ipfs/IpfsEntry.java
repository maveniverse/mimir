/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.node.ipfs;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.mimir.shared.impl.checksum.ChecksumEnforcer;
import eu.maveniverse.maven.mimir.shared.impl.checksum.ChecksumInputStream;
import eu.maveniverse.maven.mimir.shared.impl.node.EntrySupport;
import eu.maveniverse.maven.mimir.shared.node.RemoteEntry;
import io.ipfs.api.IPFS;
import io.ipfs.multihash.Multihash;
import java.io.IOException;
import java.io.InputStream;
import java.util.AbstractMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.eclipse.aether.spi.connector.checksum.ChecksumAlgorithmFactory;

public class IpfsEntry extends EntrySupport implements RemoteEntry {
    private final IPFS ipfs;
    private final Multihash multihash;
    private final Map<String, ChecksumAlgorithmFactory> algorithmFactories;

    public IpfsEntry(
            Map<String, String> metadata,
            Map<String, String> checksums,
            IPFS ipfs,
            Multihash multihash,
            Map<String, ChecksumAlgorithmFactory> algorithmFactories) {
        super(metadata, checksums);
        this.ipfs = requireNonNull(ipfs);
        this.multihash = requireNonNull(multihash);
        this.algorithmFactories = requireNonNull(algorithmFactories);
    }

    @Override
    public void handleContent(IOConsumer consumer) throws IOException {
        requireNonNull(consumer);
        try (InputStream inputStream = new ChecksumInputStream(
                ipfs.catStream(multihash),
                algorithmFactories.entrySet().stream()
                        .map(e -> new AbstractMap.SimpleEntry<>(
                                e.getKey(), e.getValue().getAlgorithm()))
                        .collect(Collectors.toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue)),
                new ChecksumEnforcer(checksums))) {
            consumer.accept(inputStream);
        }
    }
}
