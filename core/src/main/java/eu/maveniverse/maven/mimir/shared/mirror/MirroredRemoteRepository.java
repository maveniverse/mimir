/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.shared.mirror;

import java.util.List;
import org.eclipse.aether.repository.RemoteRepository;

/**
 * Mirrored remote repository.
 */
public interface MirroredRemoteRepository {
    /**
     * The mirrored remote repository, never {@code null}.
     */
    RemoteRepository remoteRepository();

    /**
     * The mirrors in specific order (ie best to worst, closest to farthest), never {@code null}.
     */
    List<RemoteRepository> remoteRepositoryMirrors();
}
