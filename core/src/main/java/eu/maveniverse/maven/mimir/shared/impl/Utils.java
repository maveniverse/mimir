/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.shared.impl;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.mimir.shared.node.Entry;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.stream.Collectors;

public final class Utils {
    private Utils() {}

    private static final String METADATA_PREFIX = "m.";
    private static final String CHECKSUM_PREFIX = "c.";

    public static Map<String, String> mergeEntry(Entry entry) {
        return mergeEntry(entry.metadata(), entry.checksums());
    }

    public static Map<String, String> mergeEntry(Map<String, String> metadata, Map<String, String> checksums) {
        HashMap<String, String> merged = new HashMap<>();
        metadata.forEach((k, v) -> merged.put(METADATA_PREFIX + k, v));
        checksums.forEach((k, v) -> merged.put(CHECKSUM_PREFIX + k, v));
        return merged;
    }

    private static Map<String, String> split(Map<String, String> merged, String prefix) {
        HashMap<String, String> result = new HashMap<>();
        merged.entrySet().stream()
                .filter(e -> e.getKey().startsWith(prefix))
                .forEach(e -> result.put(e.getKey().substring(prefix.length()), e.getValue()));
        return result;
    }

    public static Map<String, String> splitMetadata(Map<String, String> merged) {
        return split(merged, METADATA_PREFIX);
    }

    public static Map<String, String> splitChecksums(Map<String, String> merged) {
        return split(merged, CHECKSUM_PREFIX);
    }

    /**
     * Converts passed in {@link Properties} to mutable plain {@link HashMap}.
     */
    public static HashMap<String, String> toMap(Properties properties) {
        requireNonNull(properties, "properties");
        return properties.entrySet().stream()
                .collect(Collectors.toMap(
                        e -> String.valueOf(e.getKey()),
                        e -> String.valueOf(e.getValue()),
                        (prev, next) -> next,
                        HashMap::new));
    }

    /**
     * Performs a hard-linking (if on same volume), otherwise plain copies file contents. Does not check for
     * any precondition (source exists and is regular file, destination does not exist), it is caller job.
     */
    public static void copyOrLink(Path src, Path dest) throws IOException {
        if (Objects.equals(Files.getFileStore(src), Files.getFileStore(dest.getParent()))) {
            Files.createLink(dest, src);
        } else {
            Files.copy(src, dest);
            Files.setLastModifiedTime(dest, Files.getLastModifiedTime(src));
        }
    }

    /**
     * Discovers artifact version.
     */
    public static String discoverArtifactVersion(
            ClassLoader classLoader, String groupId, String artifactId, String defVersion) {
        String version = defVersion;
        String resource = "META-INF/maven/" + groupId + "/" + artifactId + "/pom.properties";
        final Properties props = new Properties();
        try (InputStream is = classLoader.getResourceAsStream(resource)) {
            if (is != null) {
                props.load(is);
            }
            version = props.getProperty("version", defVersion);
        } catch (IOException e) {
            // fall through
        }
        if (version != null) {
            version = version.trim();
            if (version.startsWith("${")) {
                version = defVersion;
            }
        }
        return version;
    }
}
