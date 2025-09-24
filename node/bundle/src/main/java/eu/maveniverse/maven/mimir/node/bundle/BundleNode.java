/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.node.bundle;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.mimir.shared.impl.node.NodeSupport;
import eu.maveniverse.maven.mimir.shared.node.LocalNode;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class BundleNode extends NodeSupport implements LocalNode {
    private final List<Bundle> bundles;
    private final List<String> checksumAlgorithms;

    public BundleNode(List<Bundle> bundles) {
        super(BundleNodeConfig.NAME);
        this.bundles = requireNonNull(bundles);
        this.checksumAlgorithms = List.of("SHA-1");
    }

    @Override
    public List<String> checksumAlgorithms() {
        return checksumAlgorithms;
    }

    @Override
    public Optional<BundleEntry> locate(URI key) throws IOException {
        checkClosed();
        Optional<BundleEntry> result = Optional.empty();
        for (Bundle bundle : bundles) {
            result = bundle.locate(key);
            if (result.isPresent()) {
                return result;
            }
        }
        return result;
    }

    @Override
    public BundleEntry store(URI key, Path file, Map<String, String> md, Map<String, String> checksums) {
        checkClosed();
        throw new IllegalStateException("bundle node cannot store");
    }

    @Override
    protected void doClose() throws IOException {
        ArrayList<IOException> exceptions = new ArrayList<>();
        for (Bundle bundle : bundles) {
            try {
                bundle.close();
            } catch (IOException e) {
                exceptions.add(e);
            }
        }
        if (!exceptions.isEmpty()) {
            IOException e = new IOException("cannot close bundle nodes");
            exceptions.forEach(e::addSuppressed);
            throw e;
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + bundles + ")";
    }
}
