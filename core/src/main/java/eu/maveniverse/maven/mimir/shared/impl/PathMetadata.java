package eu.maveniverse.maven.mimir.shared.impl;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.mimir.shared.CacheEntry;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public final class PathMetadata implements CacheEntry.Metadata {
    private final long contentLength;
    private final long lastModified;
    private final Map<String, String> checksums;

    public PathMetadata(Path cacheFile, Map<String, String> checksums) throws IOException {
        requireNonNull(cacheFile, "cacheFile");
        requireNonNull(checksums, "checksums");
        this.contentLength = Files.size(cacheFile);
        this.lastModified = Files.getLastModifiedTime(cacheFile).toMillis();
        this.checksums = checksums;
    }

    @Override
    public long contentLength() {
        return contentLength;
    }

    @Override
    public long lastModified() {
        return lastModified;
    }

    @Override
    public Map<String, String> checksums() {
        return checksums;
    }
}
