/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.node.ipfs;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.mimir.shared.SessionConfig;
import eu.maveniverse.maven.mimir.shared.node.RemoteNodeFactory;
import eu.maveniverse.maven.shared.core.component.ComponentSupport;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import org.eclipse.aether.spi.connector.checksum.ChecksumAlgorithmFactory;

@Singleton
@Named(IpfsNodeConfig.NAME)
public class IpfsNodeFactory extends ComponentSupport implements RemoteNodeFactory<IpfsNode> {
    private final Map<String, ChecksumAlgorithmFactory> checksumFactories;

    @Inject
    public IpfsNodeFactory(Map<String, ChecksumAlgorithmFactory> checksumFactories) {
        this.checksumFactories = requireNonNull(checksumFactories, "checksumFactories");
    }

    @Override
    public Optional<IpfsNode> createRemoteNode(SessionConfig sessionConfig) throws IOException {
        requireNonNull(sessionConfig, "config");

        try {
            IpfsNodeConfig cfg = IpfsNodeConfig.with(sessionConfig);
            if (!cfg.enabled()) {
                logger.info("IPFS is disabled");
                return Optional.empty();
            }
            return Optional.of(new IpfsNode(cfg.multiaddr(), cfg.checksumAlgorithms(), checksumFactories));
        } catch (Exception e) {
            throw new IOException("Failed to create JChannel", e);
        }
    }
}
