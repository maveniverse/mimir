/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.shared.impl;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.mimir.shared.SessionConfig;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.RepositoryPolicy;

/**
 * Utilities related to parsing some more complex user or configuration inputs.
 */
public final class ParseUtils {
    private ParseUtils() {}

    // BundleSource

    /**
     * Bundle source: carries a {@link RemoteRepository} and {@link Artifact}. If the artifact has file set
     * ({@link Artifact#getFile()} is not {@code null}), user used a file path. Otherwise, the artifact should be
     * resolved from given remote repository.
     */
    public record ArtifactSource(RemoteRepository remoteRepository, Artifact artifact) {}

    /**
     * Central repository.
     */
    public static final RemoteRepository CENTRAL = new RemoteRepository.Builder(
                    "central", "default", "https://repo.maven.apache.org/maven2/")
            .setReleasePolicy(new RepositoryPolicy(
                    true, RepositoryPolicy.UPDATE_POLICY_NEVER, RepositoryPolicy.CHECKSUM_POLICY_WARN))
            .setSnapshotPolicy(new RepositoryPolicy(
                    false, RepositoryPolicy.UPDATE_POLICY_NEVER, RepositoryPolicy.CHECKSUM_POLICY_WARN))
            .build();

    /**
     * Parses a sources string into list of {@link ArtifactSource}s. The string may contain multiple source definition
     * strings separated by comma (",") or semicolon (";"). Each source definition must have the following format:
     * {@code gav@repo}. The "gav" part corresponds to {@code <groupId>:<artifactId>[:<extension>[:<classifier>]]:<version>}
     * string, while "repo" part may be equal to "central" string, denoting Maven Central repository, or may be in
     * format of {@code id::url}, the usual remote repository definition string.
     */
    public static List<ArtifactSource> parseBundleSources(SessionConfig sessionConfig, String sourcesString) {
        requireNonNull(sourcesString);
        String[] sources = sourcesString.split("[;,]");
        ArrayList<ArtifactSource> artifactSources = new ArrayList<>();
        for (String source : sources) {
            RemoteRepository remoteRepository;
            Artifact artifact;
            String[] sourceParts = source.split("@");
            if (sourceParts.length != 2) {
                throw new IllegalArgumentException(
                        "Invalid bundle sources element (must have form of 'gav@repo'): " + source);
            }
            remoteRepository = parseRemoteRepositoryString(sessionConfig, sourceParts[1]);
            artifact = parseArtifactString(sessionConfig, sourceParts[0]);
            artifactSources.add(new ArtifactSource(remoteRepository, artifact));
        }
        return artifactSources;
    }

    /**
     * Parses artifact string into {@link Artifact}. If string is in form of {@code "G:A[:E[:C]]:V"}, an artifact
     * without file will be created. Otherwise, the string will be resolved against {@link SessionConfig#basedir()}
     * and if resolved path points to existing file, an "artificial" artifact will be created and have the file
     * attached.
     */
    public static Artifact parseArtifactString(SessionConfig sessionConfig, String sourcesString) {
        requireNonNull(sourcesString);
        Artifact artifact;
        if (sourcesString.contains(":")) {
            // assume artifact
            artifact = new DefaultArtifact(sourcesString);
        } else {
            // assume file
            Path path = sessionConfig.basedir().resolve(sourcesString).normalize();
            if (Files.isRegularFile(path)) {
                String artifactId = path.getFileName().toString().replaceAll("\\.", "_");
                artifact = new DefaultArtifact("local:" + artifactId + ":1.0").setFile(path.toFile());
            } else {
                throw new IllegalArgumentException("Invalid artifact path; non existent file: " + path);
            }
        }
        return artifact;
    }

    /**
     * Parses a remote repository string in form of {@code "id::url"} into {@link RemoteRepository}.
     */
    public static RemoteRepository parseRemoteRepositoryString(SessionConfig sessionConfig, String sourcesString) {
        requireNonNull(sourcesString);
        if (CENTRAL.getId().equals(sourcesString)) {
            return CENTRAL;
        }
        String[] repoParts = sourcesString.split("::");
        if (repoParts.length == 2) {
            // modern id::url
            return new RemoteRepository.Builder(repoParts[0], "default", repoParts[1]).build();
        }
        if (repoParts.length == 3) {
            // legacy id::default::url
            return new RemoteRepository.Builder(repoParts[0], "default", repoParts[2]).build();
        } else {
            throw new IllegalArgumentException(
                    "Invalid remote repository element (must have form of 'id::url'): " + sourcesString);
        }
    }
}
