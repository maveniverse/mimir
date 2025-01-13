package eu.maveniverse.maven.mimir.shared.impl;

import eu.maveniverse.maven.mimir.shared.node.LocalCacheEntry;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import org.eclipse.aether.util.FileUtils;

public class PathCacheEntry implements LocalCacheEntry {
    private final String origin;
    private final Metadata metadata;
    private final Path cacheFile;

    PathCacheEntry(String origin, Metadata metadata, Path cacheFile) {
        this.origin = origin;
        this.metadata = metadata;
        this.cacheFile = cacheFile;
    }

    @Override
    public String origin() {
        return origin;
    }

    @Override
    public Metadata metadata() {
        return metadata;
    }

    @Override
    public void transferTo(Path file) throws IOException {
        Files.deleteIfExists(file);
        try (FileUtils.CollocatedTempFile f = FileUtils.newTempFile(file)) {
            Utils.copyOrLink(cacheFile, f.getPath());
            f.move();
        }
    }

    @Override
    public FileChannel openReadableByteChannel() throws IOException {
        return FileChannel.open(cacheFile, StandardOpenOption.READ);
    }
}
