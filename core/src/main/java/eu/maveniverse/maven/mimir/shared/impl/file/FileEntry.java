package eu.maveniverse.maven.mimir.shared.impl.file;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.mimir.shared.impl.EntrySupport;
import eu.maveniverse.maven.mimir.shared.impl.Utils;
import eu.maveniverse.maven.mimir.shared.node.LocalEntry;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.eclipse.aether.util.FileUtils;

class FileEntry extends EntrySupport implements LocalEntry {
    private final Path path;

    public FileEntry(Map<String, String> metadata, Path path) {
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
