package eu.maveniverse.maven.mimir.shared.impl.file;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.mimir.shared.impl.EntrySupport;
import eu.maveniverse.maven.mimir.shared.impl.Utils;
import eu.maveniverse.maven.mimir.shared.node.Entry;
import eu.maveniverse.maven.mimir.shared.node.SystemEntry;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import org.eclipse.aether.util.FileUtils;

public final class FileEntry extends EntrySupport implements SystemEntry {
    public static FileEntry createEntry(Path file, Map<String, String> metadata) throws IOException {
        HashMap<String, String> md = new HashMap<>(metadata);
        md.put(Entry.CONTENT_LENGTH, Long.toString(Files.size(file)));
        md.put(
                Entry.CONTENT_LAST_MODIFIED,
                Long.toString(Files.getLastModifiedTime(file).toMillis()));
        return new FileEntry(md, file);
    }

    private final Path path;

    private FileEntry(Map<String, String> metadata, Path path) {
        super(metadata);
        this.path = requireNonNull(path, "path");
    }

    @Override
    public InputStream openStream() throws IOException {
        return Files.newInputStream(path);
    }

    @Override
    public void transferTo(Path file) throws IOException {
        Files.deleteIfExists(file);
        try (FileUtils.CollocatedTempFile f = FileUtils.newTempFile(file)) {
            Utils.copyOrLink(path, f.getPath());
            f.move();
        }
    }
}
