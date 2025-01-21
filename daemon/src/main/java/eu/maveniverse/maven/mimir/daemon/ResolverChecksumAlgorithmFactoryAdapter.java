/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.daemon;

import eu.maveniverse.maven.mimir.shared.checksum.ChecksumAlgorithmFactory;
import eu.maveniverse.maven.mimir.shared.impl.checksum.ChecksumAlgorithmFactoryAdapter;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

@Named
public class ResolverChecksumAlgorithmFactoryAdapter implements Provider<Map<String, ChecksumAlgorithmFactory>> {
    private final Map<String, org.eclipse.aether.spi.connector.checksum.ChecksumAlgorithmFactory>
            checksumAlgorithmFactories;

    @Inject
    public ResolverChecksumAlgorithmFactoryAdapter(
            Map<String, org.eclipse.aether.spi.connector.checksum.ChecksumAlgorithmFactory>
                    checksumAlgorithmFactories) {
        this.checksumAlgorithmFactories = checksumAlgorithmFactories;
    }

    @Override
    public Map<String, ChecksumAlgorithmFactory> get() {
        HashMap<String, ChecksumAlgorithmFactory> checksumAlgorithmFactoryMap = new HashMap<>();
        System.out.println(checksumAlgorithmFactories.keySet());
        checksumAlgorithmFactories.forEach(
                (k, v) -> checksumAlgorithmFactoryMap.put(k, new ChecksumAlgorithmFactoryAdapter(v)));
        return Map.copyOf(checksumAlgorithmFactoryMap);
    }
}
