/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.shared.naming;

import static java.util.Objects.requireNonNull;

import java.net.URI;
import java.util.function.Predicate;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.util.StringDigestUtil;
import org.eclipse.aether.util.artifact.ArtifactIdUtils;

/**
 * Keys are {@link URI} instances, built from various inputs.
 */
public final class UriEncoders {
    private UriEncoders() {}

    private static final Predicate<RemoteRepository> CENTRAL_PREDICATE = RemoteRepositories.centralDirectOnly();

    /**
     * Ensures string is not {@code null} nor empty after trimming whitespace.
     */
    private static String requireNonEmpty(String string) {
        String result = requireNonNull(string, "string must be non-null").trim();
        if (result.isEmpty()) {
            throw new IllegalArgumentException("string must be non-empty");
        }
        return result;
    }

    /**
     * Provides default and simplistic "container" implementation. Repository must not be {@code null}.
     */
    public static String container(RemoteRepository repository) {
        requireNonNull(repository);
        if (CENTRAL_PREDICATE.test(repository)) {
            return RemoteRepositories.CENTRAL_REPOSITORY_ID;
        } else {
            return repository.getId() + "-" + StringDigestUtil.sha1(repository.getUrl());
        }
    }

    /**
     * Repository and artifact -> URI. No input parameter may be {@code null}.
     */
    public static URI artifactKeyBuilder(RemoteRepository repository, Artifact artifact) {
        return artifactKeyBuilder(container(repository), artifact);
    }

    /**
     * Container name and artifact -> URI. No input parameter may be {@code null}.
     */
    public static URI artifactKeyBuilder(String repository, Artifact artifact) {
        return URI.create(
                "mimir:artifact:" + requireNonEmpty(repository) + ":" + ArtifactIdUtils.toId(requireNonNull(artifact)));
    }

    /**
     * Container and path -> URI. No input parameter may be {@code null}.
     */
    public static URI fileKeyBuilder(String container, String path) {
        return URI.create("mimir:file:" + requireNonEmpty(container) + ":" + requireNonEmpty(path));
    }

    /**
     * CAS type and address -> URI. No input parameter may be {@code null}.
     */
    public static URI casKeyBuilder(String type, String address) {
        return URI.create("mimir:cas:" + requireNonEmpty(type) + ":" + requireNonEmpty(address));
    }
}
