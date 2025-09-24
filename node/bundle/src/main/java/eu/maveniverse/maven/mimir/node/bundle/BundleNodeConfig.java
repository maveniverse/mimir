/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.node.bundle;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.mimir.shared.SessionConfig;
import eu.maveniverse.maven.mimir.shared.impl.naming.SimpleKeyResolverFactory;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.RepositoryPolicy;

public class BundleNodeConfig {
    public static BundleNodeConfig with(SessionConfig sessionConfig) throws IOException {
        requireNonNull(sessionConfig, "config");

        ArrayList<BundleSource> bundleSources = new ArrayList<>();

        String keyResolver = SimpleKeyResolverFactory.NAME;
        if (sessionConfig.effectiveProperties().containsKey("mimir.bundle.keyResolver")) {
            keyResolver = sessionConfig.effectiveProperties().get("bundle.file.keyResolver");
        }

        if (sessionConfig.effectiveProperties().containsKey("mimir.bundle.sources")) {
            // comma separated values in form of "g:a:v@id::url" or "g:a:v@central"
            String[] sources = sessionConfig
                    .effectiveProperties()
                    .get("mimir.bundle.sources")
                    .split("[;,]");
            for (String source : sources) {
                RemoteRepository remoteRepository;
                Artifact artifact;
                if (source.endsWith("@central")) {
                    remoteRepository = CENTRAL;
                    artifact = new DefaultArtifact(source.substring(0, source.length() - "@central".length()));
                } else {
                    String[] sourceParts = source.split("@");
                    if (sourceParts.length != 2) {
                        throw new IllegalArgumentException("Invalid mimir.bundle.sources configuration: " + source);
                    }
                    String[] repoParts = sourceParts[1].split("::");
                    if (repoParts.length != 2) {
                        throw new IllegalArgumentException(
                                "Invalid mimir.bundle.sources repository configuration: " + source);
                    }
                    remoteRepository = new RemoteRepository.Builder("central", repoParts[0], repoParts[1]).build();
                    artifact = new DefaultArtifact(sourceParts[0]);
                }
                bundleSources.add(new BundleSource(remoteRepository, artifact, keyResolver));
            }
        }
        return new BundleNodeConfig(bundleSources);
    }

    public static final String NAME = "bundle";

    private static final RemoteRepository CENTRAL = new RemoteRepository.Builder(
                    "central", "default", "https://repo.maven.apache.org/maven2/")
            .setReleasePolicy(new RepositoryPolicy(
                    true, RepositoryPolicy.UPDATE_POLICY_NEVER, RepositoryPolicy.CHECKSUM_POLICY_WARN))
            .setSnapshotPolicy(new RepositoryPolicy(
                    false, RepositoryPolicy.UPDATE_POLICY_NEVER, RepositoryPolicy.CHECKSUM_POLICY_WARN))
            .build();

    private final List<BundleSource> bundleSources;

    private BundleNodeConfig(List<BundleSource> bundleSources) {
        this.bundleSources = bundleSources;
    }

    public List<BundleSource> bundleSources() {
        return bundleSources;
    }

    public record BundleSource(RemoteRepository remoteRepository, Artifact artifact, String keyResolver) {}
}
