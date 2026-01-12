/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.shared.naming;

import java.net.URI;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;

/**
 * Builds from {@link URI} instances various keys.
 */
public final class UriDecoders {
    private UriDecoders() {}

    public static Keys.Key apply(URI uri) {
        if (uri.isOpaque()) {
            if ("mimir".equals(uri.getScheme())) {
                String ssp = uri.getSchemeSpecificPart();
                if (ssp.startsWith("artifact:")) {
                    String[] bits = ssp.substring(9).split(":", 2);
                    if (bits.length == 2) {
                        String container = bits[0];
                        Artifact artifact = new DefaultArtifact(bits[1]);
                        return new Keys.ArtifactKey(uri, container, artifact);
                    }
                } else if (ssp.startsWith("file:")) {
                    String[] bits = ssp.substring(5).split(":", 2);
                    if (bits.length == 2) {
                        String container = bits[0];
                        String path = bits[1];
                        return new Keys.FileKey(uri, container, path);
                    }
                } else if (ssp.startsWith("cas:")) {
                    String[] bits = ssp.substring(4).split(":", 2);
                    if (bits.length == 2) {
                        String type = bits[0];
                        String address = bits[1];
                        return new Keys.CasKey(uri, type, address);
                    }
                }
            }
        }
        throw new IllegalArgumentException("Unexpected URI");
    }
}
