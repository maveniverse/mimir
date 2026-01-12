package eu.maveniverse.maven.mimir.node.file;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import eu.maveniverse.maven.mimir.shared.SessionConfig;
import eu.maveniverse.maven.mimir.shared.naming.UriEncoders;
import eu.maveniverse.maven.mimir.shared.node.LocalEntry;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.internal.impl.checksum.Sha1ChecksumAlgorithmFactory;
import org.eclipse.aether.internal.impl.checksum.Sha512ChecksumAlgorithmFactory;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.spi.connector.checksum.ChecksumAlgorithmHelper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class FileNodeTest {
    private final RemoteRepository central =
            new RemoteRepository.Builder("central", "default", "https://repo.maven.apache.org/maven2/").build();
    private final Artifact junit = new DefaultArtifact("junit:junit:3.13.2");

    @Test
    void smoke(@TempDir Path basedir, @TempDir Path workdir) throws Exception {
        SessionConfig sessionConfig = SessionConfig.defaults().basedir(basedir).build();
        try (FileNode fileNode = new FileNodeFactory(Map.of(
                        Sha1ChecksumAlgorithmFactory.NAME,
                        new Sha1ChecksumAlgorithmFactory(),
                        Sha512ChecksumAlgorithmFactory.NAME,
                        new Sha512ChecksumAlgorithmFactory()))
                .createSystemNode(sessionConfig)) {
            Optional<FileEntry> entry = fileNode.locate(UriEncoders.artifactKeyBuilder(central, junit));
            assertFalse(entry.isPresent());

            byte[] data = "Hello World!".getBytes(StandardCharsets.UTF_8);
            Path temp = Files.createTempFile("mimir", "tmp");
            Files.write(temp, data, StandardOpenOption.TRUNCATE_EXISTING);
            Map<String, String> checksums = ChecksumAlgorithmHelper.calculate(
                    data, Arrays.asList(new Sha1ChecksumAlgorithmFactory(), new Sha512ChecksumAlgorithmFactory()));
            fileNode.store(UriEncoders.artifactKeyBuilder(central, junit), temp, Map.of(), checksums);

            entry = fileNode.locate(UriEncoders.artifactKeyBuilder(central, junit));
            assertTrue(entry.isPresent());
            LocalEntry localEntry = entry.orElseThrow();
            assertEquals(12, localEntry.getContentLength());
            assertEquals(
                    "2ef7bde608ce5404e97d5f042f95f89f1c232871",
                    localEntry.checksums().get(Sha1ChecksumAlgorithmFactory.NAME));
            assertEquals(
                    "861844d6704e8573fec34d967e20bcfef3d424cf48be04e6dc08f2bd58c729743371015ead891cc3cf1c9d34b49264b510751b1ff9e537937bc46b5d6ff4ecc8",
                    localEntry.checksums().get(Sha512ChecksumAlgorithmFactory.NAME));
            System.out.println(localEntry.metadata());

            Path target = workdir.resolve("target.file");
            localEntry.transferTo(target);
            assertArrayEquals(Files.readAllBytes(target), data);
        }
    }

    @Test
    void smokeNoLink(@TempDir Path basedir, @TempDir Path workdir) throws Exception {
        SessionConfig sessionConfig = SessionConfig.defaults()
                .basedir(basedir)
                .setUserProperty("mimir.file.mayLink", "false")
                .build();
        try (FileNode fileNode = new FileNodeFactory(Map.of(
                        Sha1ChecksumAlgorithmFactory.NAME,
                        new Sha1ChecksumAlgorithmFactory(),
                        Sha512ChecksumAlgorithmFactory.NAME,
                        new Sha512ChecksumAlgorithmFactory()))
                .createSystemNode(sessionConfig)) {
            Optional<FileEntry> entry = fileNode.locate(UriEncoders.artifactKeyBuilder(central, junit));
            assertFalse(entry.isPresent());

            byte[] data = "Hello World!".getBytes(StandardCharsets.UTF_8);
            Path temp = Files.createTempFile("mimir", "tmp");
            Files.write(temp, data, StandardOpenOption.TRUNCATE_EXISTING);
            Map<String, String> checksums = ChecksumAlgorithmHelper.calculate(
                    data, Arrays.asList(new Sha1ChecksumAlgorithmFactory(), new Sha512ChecksumAlgorithmFactory()));
            fileNode.store(UriEncoders.artifactKeyBuilder(central, junit), temp, Map.of(), checksums);

            entry = fileNode.locate(UriEncoders.artifactKeyBuilder(central, junit));
            assertTrue(entry.isPresent());
            LocalEntry localEntry = entry.orElseThrow();
            assertEquals(12, localEntry.getContentLength());
            assertEquals(
                    "2ef7bde608ce5404e97d5f042f95f89f1c232871",
                    localEntry.checksums().get(Sha1ChecksumAlgorithmFactory.NAME));
            assertEquals(
                    "861844d6704e8573fec34d967e20bcfef3d424cf48be04e6dc08f2bd58c729743371015ead891cc3cf1c9d34b49264b510751b1ff9e537937bc46b5d6ff4ecc8",
                    localEntry.checksums().get(Sha512ChecksumAlgorithmFactory.NAME));
            System.out.println(localEntry.metadata());

            Path target = workdir.resolve("target.file");
            localEntry.transferTo(target);
            assertArrayEquals(Files.readAllBytes(target), data);
        }
    }

    @Test
    void sharedAccess(@TempDir Path basedir, @TempDir Path workdir) throws Exception {
        SessionConfig sessionConfig = SessionConfig.defaults()
                .basedir(basedir)
                .setUserProperty("mimir.file.exclusiveAccess", "false")
                .build();
        FileNodeFactory fileNodeFactory = new FileNodeFactory(Map.of(
                Sha1ChecksumAlgorithmFactory.NAME,
                new Sha1ChecksumAlgorithmFactory(),
                Sha512ChecksumAlgorithmFactory.NAME,
                new Sha512ChecksumAlgorithmFactory()));
        try (FileNode fileNode1 = fileNodeFactory.createSystemNode(sessionConfig);
                FileNode fileNode2 = fileNodeFactory.createSystemNode(sessionConfig)) {
            // should be ok
        }
    }

    @Test
    void exclusiveAccess(@TempDir Path basedir, @TempDir Path workdir) throws Exception {
        SessionConfig sessionConfig = SessionConfig.defaults()
                .basedir(basedir)
                .setUserProperty("mimir.file.exclusiveAccess", "true")
                .build();
        FileNodeFactory fileNodeFactory = new FileNodeFactory(Map.of(
                Sha1ChecksumAlgorithmFactory.NAME,
                new Sha1ChecksumAlgorithmFactory(),
                Sha512ChecksumAlgorithmFactory.NAME,
                new Sha512ChecksumAlgorithmFactory()));
        try (FileNode fileNode = fileNodeFactory.createSystemNode(sessionConfig)) {
            assertThrows(IOException.class, () -> fileNodeFactory.createSystemNode(sessionConfig));
        }
    }
}
