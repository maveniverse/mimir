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
import eu.maveniverse.maven.mimir.shared.impl.naming.SimpleKeyMapperFactory;
import eu.maveniverse.maven.mimir.shared.naming.KeyResolver;
import eu.maveniverse.maven.mimir.shared.naming.KeyResolverFactory;
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
    private final Map<String, KeyResolverFactory> keyResolverFactories;

    @Inject
    public BundleNodeFactory(
            @Nullable RepositorySystem repositorySystem, Map<String, KeyResolverFactory> keyResolverFactories) {
        this.repositorySystem = repositorySystem;
        this.keyResolverFactories = requireNonNull(keyResolverFactories, "keyResolverFactories");
    }

    @Override
    public Optional<BundleNode> createNode(SessionConfig sessionConfig) throws IOException {
        requireNonNull(sessionConfig, "config");
        BundleNodeConfig bundleNodeConfig = BundleNodeConfig.with(sessionConfig);
        if (repositorySystem != null
                && sessionConfig.repositorySystemSession().isPresent()
                && !bundleNodeConfig.bundleSources().isEmpty()) {
            ArrayList<Bundle> bundles = new ArrayList<>();
            for (BundleNodeConfig.BundleSource bundleSource : bundleNodeConfig.bundleSources()) {
                KeyResolverFactory keyResolverFactory = keyResolverFactories.get(bundleSource.keyResolver());
                if (keyResolverFactory == null) {
                    throw new IllegalArgumentException("Unknown keyResolver: " + bundleSource.keyResolver());
                }
                KeyResolver keyResolver =
                        requireNonNull(keyResolverFactory.createKeyResolver(sessionConfig), "keyResolver");

                RemoteRepository remoteRepository = bundleSource.remoteRepository();

                try {
                    ArtifactResult artifactResult = repositorySystem.resolveArtifact(
                            sessionConfig.repositorySystemSession().orElseThrow(),
                            new ArtifactRequest(
                                    bundleSource.artifact(),
                                    Collections.singletonList(bundleSource.remoteRepository()),
                                    "mimir-bundle-node"));
                    Artifact artifact = artifactResult.getArtifact();
                    if (artifactResult.isResolved()) {
                        bundles.add(new Bundle(
                                SimpleKeyMapperFactory.container(remoteRepository),
                                ArtifactIdUtils.toId(artifact),
                                FileSystems.newFileSystem(
                                        URI.create("jar:"
                                                + artifact.getFile().toPath().toUri()),
                                        Map.of("create", "false"),
                                        null),
                                keyResolver));
                    }
                } catch (ArtifactResolutionException e) {
                    throw new IOException("Unable to resolve artifact " + bundleSource.artifact(), e);
                }
            }
            if (!bundles.isEmpty()) {
                return Optional.of(new BundleNode(bundles));
            }
        }
        return Optional.empty();
    }
}
