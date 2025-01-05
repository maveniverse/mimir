package eu.maveniverse.maven.mimir.shared;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class VolumeTest {
    @Test
    void smoke(@TempDir Path tempDir) throws IOException {
        Path home = Path.of(System.getProperty("user.home"));

        FileStore homeStore = Files.getFileStore(home);
        FileStore tempStore = Files.getFileStore(tempDir);

        System.out.println(homeStore);
        System.out.println(tempStore);
    }
}
