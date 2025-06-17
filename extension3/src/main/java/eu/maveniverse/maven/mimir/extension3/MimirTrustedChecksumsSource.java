/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.extension3;

import eu.maveniverse.maven.mimir.shared.Entry;
import eu.maveniverse.maven.mimir.shared.MimirUtils;
import eu.maveniverse.maven.mimir.shared.Session;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.inject.Named;
import javax.inject.Singleton;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.repository.ArtifactRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.spi.checksums.TrustedChecksumsSource;
import org.eclipse.aether.spi.connector.checksum.ChecksumAlgorithmFactory;

@Singleton
@Named
public class MimirTrustedChecksumsSource implements TrustedChecksumsSource {
    @Override
    public Map<String, String> getTrustedArtifactChecksums(
            RepositorySystemSession session,
            Artifact artifact,
            ArtifactRepository artifactRepository,
            List<ChecksumAlgorithmFactory> checksumAlgorithmFactories) {
        if (artifactRepository instanceof RemoteRepository remoteRepository) {
            Optional<Session> sessionOptional = MimirUtils.mayGetSession(session);
            if (sessionOptional.isPresent()) {
                Session ms = sessionOptional.orElseThrow();
                if (ms.repositorySupported(remoteRepository) && ms.artifactSupported(artifact)) {
                    try {
                        Optional<Entry> entry = ms.locate(remoteRepository, artifact);
                        if (entry.isPresent()) {
                            Entry cacheEntry = entry.orElseThrow();
                            HashMap<String, String> result = new HashMap<>();
                            for (ChecksumAlgorithmFactory checksumAlgorithmFactory : checksumAlgorithmFactories) {
                                String checksum = cacheEntry.checksums().get(checksumAlgorithmFactory.getName());
                                if (checksum != null) {
                                    result.put(checksumAlgorithmFactory.getName(), checksum);
                                }
                            }
                            return result;
                        }
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                }
            }
        }
        return Collections.emptyMap();
    }

    @Override
    public Writer getTrustedArtifactChecksumsWriter(RepositorySystemSession session) {
        return null;
    }
}
