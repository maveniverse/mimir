/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.shared.naming;

import static java.util.Objects.requireNonNull;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Predicate;
import org.eclipse.aether.repository.RemoteRepository;

/**
 * Some handy predicates for {@link RemoteRepository} instances.
 */
public final class RemoteRepositories {
    public static final String CENTRAL_REPOSITORY_ID = "central";

    public static final Set<String> DEFAULT = Set.of(String.format(
            "%s(%s;%s;%s)",
            RemoteRepositories.CENTRAL_REPOSITORY_ID,
            RemoteRepositories.MOD_DIRECT_ONLY,
            RemoteRepositories.MOD_RELEASE_ONLY,
            RemoteRepositories.MOD_HTTPS_ONLY));

    public static final String MOD_DIRECT_ONLY = "directOnly";
    public static final String MOD_RELEASE_ONLY = "releaseOnly";
    public static final String MOD_HTTPS_ONLY = "httpsOnly";

    private static final Collection<String> CENTRAL_URLS = List.of(
            "https://repo.maven.apache.org/maven2",
            "https://repo1.maven.org/maven2",
            "https://maven-central.storage-download.googleapis.com/maven2");

    private RemoteRepositories() {}

    public static Predicate<RemoteRepository> repositoryPredicate(Collection<String> repositories) {
        if (repositories == null || repositories.isEmpty()) {
            throw new IllegalStateException("No repositories to handle");
        }
        Predicate<RemoteRepository> result = null;
        for (String repoSpec : repositories) {
            Predicate<RemoteRepository> repoPredicate;
            if (!repoSpec.contains("(") && !repoSpec.contains(")")) {
                if ("*".equals(repoSpec)) {
                    repoPredicate = release();
                } else {
                    repoPredicate = idOnly(repoSpec).and(release());
                }
            } else if (repoSpec.contains("(") && repoSpec.contains(")")) {
                String repoId = repoSpec.substring(0, repoSpec.indexOf("("));
                if ("*".equals(repoId)) {
                    repoPredicate = r -> true;
                } else {
                    repoPredicate = idOnly(repoId);
                }
                if (repoSpec.contains(MOD_HTTPS_ONLY)) {
                    repoPredicate = repoPredicate.and(httpsOnly());
                }
                if (repoSpec.contains(MOD_DIRECT_ONLY)) {
                    repoPredicate = repoPredicate.and(directOnly());
                }
                if (repoSpec.contains(MOD_RELEASE_ONLY)) {
                    repoPredicate = repoPredicate.and(releaseOnly());
                } else {
                    repoPredicate = repoPredicate.and(release());
                }
            } else {
                throw new IllegalStateException("Invalid repository spec: " + repoSpec);
            }
            if (result == null) {
                result = repoPredicate;
            } else {
                result = result.or(repoPredicate);
            }
        }
        return result;
    }

    /**
     * For simplicity's sake: this one supports ONLY Maven Central (direct access via HTTPS).
     */
    public static Predicate<RemoteRepository> centralDirectOnly() {
        return idOnly(CENTRAL_REPOSITORY_ID)
                .and(directOnly())
                .and(releaseOnly())
                .and(httpsOnly())
                .and(remoteRepository -> CENTRAL_URLS.stream()
                        .anyMatch(u -> remoteRepository.getUrl().startsWith(u)));
    }

    /**
     * Repository is supported if:
     * <ul>
     *     <li>repository.id equals to given ID</li>
     * </ul>
     */
    public static Predicate<RemoteRepository> idOnly(String repositoryId) {
        requireNonNull(repositoryId, "repositoryId");
        return remoteRepository -> repositoryId.equals(remoteRepository.getId());
    }

    /**
     * Repository is supported if:
     * <ul>
     *     <li>repository.protocol equals (case insensitive) "https"</li>
     * </ul>
     */
    public static Predicate<RemoteRepository> httpsOnly() {
        return remoteRepository ->
                remoteRepository.getProtocol().toLowerCase(Locale.ROOT).contains("https");
    }

    /**
     * Repository is supported if:
     * <ul>
     *     <li>repository is not null</li></loi>
     *     <li>repository.mirroredRepositories is empty (direct; non mirror)</li>
     *     <li>repository.repositoryManager is false</li>
     *     <li>repository.blocked is false</li>
     * </ul>
     */
    public static Predicate<RemoteRepository> directOnly() {
        return remoteRepository ->
                remoteRepository.getMirroredRepositories().isEmpty() && !remoteRepository.isRepositoryManager();
    }

    /**
     * Repository is supported if:
     * <ul>
     *     <li>repository is not null</li></loi>
     *     <li>repository.releasePolicy is enabled</li>
     *     <li>repository.snapshotPolicy is not enabled</li>
     *     <li>repository.blocked is false</li>
     * </ul>
     */
    public static Predicate<RemoteRepository> releaseOnly() {
        return remoteRepository -> remoteRepository.getPolicy(false).isEnabled()
                && !remoteRepository.getPolicy(true).isEnabled()
                && !remoteRepository.isBlocked();
    }

    /**
     * Repository is supported if:
     * <ul>
     *     <li>repository is not null</li></loi>
     *     <li>repository.releasePolicy is enabled</li>
     *     <li>repository.blocked is false</li>
     * </ul>
     */
    public static Predicate<RemoteRepository> release() {
        return remoteRepository -> remoteRepository.getPolicy(false).isEnabled() && !remoteRepository.isBlocked();
    }
}
