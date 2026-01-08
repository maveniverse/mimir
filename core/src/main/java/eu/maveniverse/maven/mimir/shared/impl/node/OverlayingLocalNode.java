/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.shared.impl.node;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.mimir.shared.node.LocalEntry;
import eu.maveniverse.maven.mimir.shared.node.LocalNode;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A wrapper local node that is able to "overlay" given local nodes in front of current local node.
 */
public class OverlayingLocalNode extends NodeSupport implements LocalNode {
    private final List<LocalNode> overlays;
    private final LocalNode localNode;

    public OverlayingLocalNode(List<LocalNode> overlays, LocalNode localNode) {
        super("overlaying");
        this.overlays = requireNonNull(overlays);
        this.localNode = requireNonNull(localNode);
    }

    @Override
    public List<String> checksumAlgorithms() throws IOException {
        return localNode.checksumAlgorithms();
    }

    @Override
    public Optional<? extends LocalEntry> locate(URI key) throws IOException {
        for (LocalNode overlay : overlays) {
            Optional<? extends LocalEntry> localEntry = overlay.locate(key);
            if (localEntry.isPresent()) {
                return localEntry;
            }
        }
        return localNode.locate(key);
    }

    @Override
    public LocalEntry store(URI key, Path file, Map<String, String> metadata, Map<String, String> checksums)
            throws IOException {
        return localNode.store(key, file, metadata, checksums);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + overlays + ", " + localNode + ")";
    }
}
