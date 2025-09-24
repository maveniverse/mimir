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
import java.io.IOException;
import java.nio.file.Path;
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

        if (sessionConfig.effectiveProperties().containsKey("mimir.bundle.sources")) {
            // comma separated values in form of "g:a:v@id::url" or "g:a:v@central"
            String[] sources = sessionConfig
                    .effectiveProperties()
                    .get("mimir.bundle.sources")
                    .split("[;,]");
            for (String source : sources) {
                RemoteRepository remoteRepository;
                Artifact artifact;
                String[] sourceParts = source.split("@");
                if (sourceParts.length != 2) {
                    throw new IllegalArgumentException("Invalid mimir.bundle.sources configuration: " + source);
                }
                if ("central".equals(sourceParts[1])) {
                    remoteRepository = CENTRAL;
                } else {
                    String[] repoParts = sourceParts[1].split("::");
                    remoteRepository = new RemoteRepository.Builder(repoParts[0], "default", repoParts[1]).build();
                }

                if (sourceParts[0].contains(":")) {
                    // assume artifact
                    artifact = new DefaultArtifact(sourceParts[0]);
                } else {
                    // assume file
                    Path path = sessionConfig.basedir().resolve(sourceParts[0]).normalize();
                    artifact = new DefaultArtifact("local:local:1.0").setFile(path.toFile());
                }
                bundleSources.add(new BundleSource(remoteRepository, artifact));
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

    public record BundleSource(RemoteRepository remoteRepository, Artifact artifact) {}
}
