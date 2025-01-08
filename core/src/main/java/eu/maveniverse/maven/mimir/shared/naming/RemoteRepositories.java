/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.shared.naming;

import static java.util.Objects.requireNonNull;

import java.util.Locale;
import java.util.function.Predicate;
import org.eclipse.aether.repository.RemoteRepository;

/**
 * Some handy predicates for {@link RemoteRepository} instances.
 */
public final class RemoteRepositories {
    private RemoteRepositories() {}

    /**
     * For simplicity's sake: this one supports ONLY Maven Central (direct access).
     * Repository is supported if:
     * <ul>
     *     <li>the {@link #httpsReleaseDirectOnlyWithId(String)} supports it; id is "central"</li>
     * </ul>
     */
    public static Predicate<RemoteRepository> centralDirectOnly() {
        return httpsReleaseDirectOnlyWithId("central");
    }

    /**
     * Repository is supported if:
     * <ul>
     *     <li>the {@link #httpsReleaseDirectOnly()} supports it</li>
     *     <li>repository.id equals to given ID</li>
     * </ul>
     */
    public static Predicate<RemoteRepository> httpsReleaseDirectOnlyWithId(String repositoryId) {
        requireNonNull(repositoryId, "repositoryId");
        return httpsReleaseDirectOnly().and((remoteRepository) -> repositoryId.equals(remoteRepository.getId()));
    }

    /**
     * Repository is supported if:
     * <ul>
     *     <li>the {@link #releaseDirectOnly()} supports it</li>
     *     <li>repository.protocol contains "https"</li>
     * </ul>
     */
    public static Predicate<RemoteRepository> httpsReleaseDirectOnly() {
        return releaseDirectOnly()
                .and((remoteRepository) ->
                        remoteRepository.getProtocol().toLowerCase(Locale.ROOT).contains("https"));
    }

    /**
     * Repository is supported if:
     * <ul>
     *     <li>repository is not null</li></loi>
     *     <li>repository.releasePolicy is enabled</li>
     *     <li>repository.snapshotPolicy is not enabled</li>
     *     <li>repository.mirroredRepositories is empty (is not a mirror w/ id="central")</li>
     *     <li>repository.repositoryManager is false</li>
     *     <li>repository.blocked is false</li>
     * </ul>
     */
    public static Predicate<RemoteRepository> releaseDirectOnly() {
        return (remoteRepository) -> remoteRepository != null
                && remoteRepository.getPolicy(false).isEnabled()
                && !remoteRepository.getPolicy(true).isEnabled()
                && remoteRepository.getMirroredRepositories().isEmpty()
                && !remoteRepository.isRepositoryManager()
                && !remoteRepository.isBlocked();
    }
}
