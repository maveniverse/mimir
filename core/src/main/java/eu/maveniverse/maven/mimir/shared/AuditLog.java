/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.shared;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.mimir.shared.impl.naming.Artifacts;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.repository.ArtifactRepository;
import org.eclipse.aether.repository.RemoteRepository;

/**
 * Appends one record per artifact resolution to one or two log files (global and optional per-project).
 * Thread-safe. Supported formats: {@code csv} and {@code jsonl}.
 */
public final class AuditLog implements Closeable {
    public static final String STATUS_RESOLVED = "resolved";
    public static final String STATUS_FAILED = "failed";

    private static final String CSV_HEADER =
            "timestamp,groupId,artifactId,version,classifier,extension,repositoryId,repositoryUrl,artifactUrl,status,context,scope";

    private final String format;
    private final List<BufferedWriter> writers;

    public AuditLog(Path globalPath, /* @Nullable */ Path projectPath, String format) throws IOException {
        requireNonNull(globalPath, "globalPath");
        requireNonNull(format, "format");
        this.format = format;
        this.writers = new ArrayList<>(2);
        writers.add(openWriter(globalPath));
        if (projectPath != null) {
            writers.add(openWriter(projectPath));
        }
    }

    private BufferedWriter openWriter(Path path) throws IOException {
        Files.createDirectories(path.getParent());
        boolean isNew = !Files.exists(path) || Files.size(path) == 0;
        BufferedWriter writer = Files.newBufferedWriter(path, StandardOpenOption.APPEND, StandardOpenOption.CREATE);
        if ("csv".equals(format) && isNew) {
            writer.write(CSV_HEADER);
            writer.newLine();
            writer.flush();
        }
        return writer;
    }

    public synchronized void record(
            ArtifactRepository repository,
            Artifact artifact,
            String status,
            String context,
            String scope,
            Map<String, String> checksums) {
        requireNonNull(repository, "repository");
        requireNonNull(artifact, "artifact");
        requireNonNull(status, "status");

        String timestamp = Instant.now().toString();
        String groupId = artifact.getGroupId();
        String artifactId = artifact.getArtifactId();
        String version = artifact.getVersion();
        String classifier = artifact.getClassifier() != null ? artifact.getClassifier() : "";
        String extension = artifact.getExtension() != null ? artifact.getExtension() : "";
        String repoId = repository.getId();
        String repoUrl = repository instanceof RemoteRepository ? ((RemoteRepository) repository).getUrl() : "";
        String artifactUrl = repoUrl.endsWith("/")
                ? repoUrl + Artifacts.artifactRepositoryPath(artifact)
                : repoUrl + "/" + Artifacts.artifactRepositoryPath(artifact);

        String line;
        if ("jsonl".equals(format)) {
            line = "{\"timestamp\":\""
                    + escapeJson(timestamp)
                    + "\",\"groupId\":\""
                    + escapeJson(groupId)
                    + "\",\"artifactId\":\""
                    + escapeJson(artifactId)
                    + "\",\"version\":\""
                    + escapeJson(version)
                    + "\",\"classifier\":\""
                    + escapeJson(classifier)
                    + "\",\"extension\":\""
                    + escapeJson(extension)
                    + "\",\"repositoryId\":\""
                    + escapeJson(repoId)
                    + "\",\"repositoryUrl\":\""
                    + escapeJson(repoUrl)
                    + "\",\"artifactUrl\":\""
                    + escapeJson(artifactUrl)
                    + "\",\"status\":\""
                    + escapeJson(status)
                    + "\",\"context\":\""
                    + escapeJson(context)
                    + "\",\"scope\":\""
                    + escapeJson(scope)
                    + "\"}";
        } else {
            line = csvField(timestamp)
                    + ","
                    + csvField(groupId)
                    + ","
                    + csvField(artifactId)
                    + ","
                    + csvField(version)
                    + ","
                    + csvField(classifier)
                    + ","
                    + csvField(extension)
                    + ","
                    + csvField(repoId)
                    + ","
                    + csvField(repoUrl)
                    + ","
                    + csvField(artifactUrl)
                    + ","
                    + csvField(status)
                    + ","
                    + csvField(context)
                    + ","
                    + csvField(scope);
        }

        try {
            for (BufferedWriter writer : writers) {
                writer.write(line);
                writer.newLine();
            }
            for (BufferedWriter writer : writers) {
                writer.flush();
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public synchronized void close() throws IOException {
        IOException first = null;
        for (BufferedWriter writer : writers) {
            try {
                writer.close();
            } catch (IOException e) {
                if (first == null) first = e;
            }
        }
        if (first != null) throw first;
    }

    private static String csvField(String value) {
        if (value == null || value.isEmpty()) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private static String escapeJson(String value) {
        if (value == null || value.isEmpty()) return "";
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
