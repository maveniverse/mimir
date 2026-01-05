/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.shared.mirror;

import eu.maveniverse.maven.mimir.shared.SessionConfig;
import eu.maveniverse.maven.mimir.shared.impl.ParseUtils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.eclipse.aether.repository.RemoteRepository;

/**
 * Some handy helpers for mirrors.
 */
public final class Mirrors {
    private Mirrors() {}

    /**
     * Parses a collection of strings in form of {@code id(id::url,id::url,...)} into map.
     */
    public static Map<String, List<RemoteRepository>> parseMirrors(SessionConfig config, Collection<String> mirrors) {
        if (mirrors == null || mirrors.isEmpty()) {
            return Map.of();
        }
        HashMap<String, List<RemoteRepository>> result = new HashMap<>();
        for (String mirrorSpec : mirrors) {
            if (mirrorSpec.indexOf("(") > 1 && mirrorSpec.endsWith(")")) {
                String repoId = mirrorSpec.substring(0, mirrorSpec.indexOf("("));
                String body = mirrorSpec.substring(mirrorSpec.indexOf("(") + 1, mirrorSpec.length() - 1);
                ArrayList<RemoteRepository> mirrorList = new ArrayList<>();
                for (String mirror : body.split("[,;]")) {
                    String spec = mirror.trim();
                    if (!spec.contains("::")) {
                        // URL alone; make it a spec with same ID as it is mirror of
                        spec = repoId + "::" + spec;
                    }
                    mirrorList.add(ParseUtils.parseRemoteRepositorySpec(config, spec));
                }
                result.put(repoId, mirrorList);
            } else {
                throw new IllegalStateException("Invalid mirror spec: " + mirrorSpec);
            }
        }
        return result;
    }
}
