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
import eu.maveniverse.maven.mimir.shared.impl.ParseUtils;
import eu.maveniverse.maven.mimir.shared.impl.naming.SimpleKeyMapperFactory;
import eu.maveniverse.maven.mimir.shared.impl.naming.SimpleKeyResolverFactory;
import eu.maveniverse.maven.mimir.shared.naming.KeyResolver;
import eu.maveniverse.maven.mimir.shared.node.LocalNodeFactory;
import eu.maveniverse.maven.shared.core.component.ComponentSupport;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystems;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.util.artifact.ArtifactIdUtils;
import org.eclipse.sisu.Nullable;

@Singleton
@Named(BundleNodeConfig.NAME)
public class BundleNodeFactory extends ComponentSupport implements LocalNodeFactory<BundleNode> {
    private final RepositorySystem repositorySystem;

    @Inject
    public BundleNodeFactory(@Nullable RepositorySystem repositorySystem) {
        this.repositorySystem = repositorySystem;
    }

    @Override
    public Optional<BundleNode> createLocalNode(SessionConfig sessionConfig) throws IOException {
        requireNonNull(sessionConfig, "config");
        BundleNodeConfig bundleNodeConfig = BundleNodeConfig.with(sessionConfig);
        if (repositorySystem != null
                && sessionConfig.repositorySystemSession().isPresent()
                && !bundleNodeConfig.bundleSources().isEmpty()) {
            // TODO: for now fixed
            KeyResolver keyResolver =
                    new SimpleKeyResolverFactory.SimpleKeyResolver(SimpleKeyResolverFactory::artifactRepositoryPath);
            ArrayList<Bundle> bundles = new ArrayList<>();
            for (ParseUtils.ArtifactSource artifactSource : bundleNodeConfig.bundleSources()) {
                RemoteRepository remoteRepository = artifactSource.remoteRepository();
                Artifact artifact = artifactSource.artifact();
                if (artifact.getFile() == null) {
                    try {
                        ArtifactResult artifactResult = repositorySystem.resolveArtifact(
                                sessionConfig.repositorySystemSession().orElseThrow(),
                                new ArtifactRequest(
                                        artifactSource.artifact(),
                                        Collections.singletonList(artifactSource.remoteRepository()),
                                        "mimir-bundle-node"));
                        artifact = artifactResult.getArtifact();
                    } catch (ArtifactResolutionException e) {
                        throw new IOException("Unable to resolve artifact " + artifactSource.artifact(), e);
                    }
                }
                bundles.add(new Bundle(
                        SimpleKeyMapperFactory.container(remoteRepository),
                        ArtifactIdUtils.toId(artifact),
                        FileSystems.newFileSystem(
                                URI.create("jar:" + artifact.getFile().toPath().toUri()),
                                Map.of("create", "false"),
                                null),
                        keyResolver));
            }
            if (!bundles.isEmpty()) {
                return Optional.of(new BundleNode(bundles));
            }
        }
        return Optional.empty();
    }
}
