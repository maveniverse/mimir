package eu.maveniverse.maven.mimir.node.minio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import eu.maveniverse.maven.mimir.shared.SessionConfig;
import eu.maveniverse.maven.mimir.shared.impl.naming.SimpleKeyMapperFactory;
import eu.maveniverse.maven.mimir.shared.impl.naming.SimpleKeyResolverFactory;
import eu.maveniverse.maven.mimir.shared.naming.KeyMapper;
import eu.maveniverse.maven.mimir.shared.node.LocalEntry;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
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
import org.eclipse.aether.repository.RepositoryPolicy;
import org.eclipse.aether.spi.connector.checksum.ChecksumAlgorithmHelper;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
public class MinioNodeTest {
    private final RemoteRepository central = new RemoteRepository.Builder(
                    "central", "default", "https://repo.maven.apache.org/maven2/")
            .setSnapshotPolicy(new RepositoryPolicy(false, "", ""))
            .build();
    private final Artifact junit = new DefaultArtifact("junit:junit:3.13.2");
    private final KeyMapper keyMapper = new SimpleKeyMapperFactory()
            .createKeyMapper(SessionConfig.defaults().build());

    @Test
    void smoke() throws Exception {
        try (MinIOContainer container = new MinIOContainer("minio/minio:RELEASE.2025-04-22T22-12-26Z")) {
            container.start();
            try (MinioClient minioClient = MinioClient.builder()
                    .endpoint(container.getS3URL())
                    .credentials(container.getUserName(), container.getPassword())
                    .build()) {
                minioClient.makeBucket(
                        MakeBucketArgs.builder().bucket(central.getId()).build());
                SessionConfig sessionConfig = SessionConfig.defaults()
                        .setUserProperty("mimir.minio.endpoint", container.getS3URL())
                        .setUserProperty("mimir.minio.accessKey", container.getUserName())
                        .setUserProperty("mimir.minio.secretKey", container.getPassword())
                        .build();
                try (MinioNode minioNode = new MinioNodeFactory(
                                Map.of(SimpleKeyResolverFactory.NAME, new SimpleKeyResolverFactory()),
                                Map.of(
                                        Sha1ChecksumAlgorithmFactory.NAME,
                                        new Sha1ChecksumAlgorithmFactory(),
                                        Sha512ChecksumAlgorithmFactory.NAME,
                                        new Sha512ChecksumAlgorithmFactory()))
                        .createSystemNode(sessionConfig)) {
                    Optional<MinioEntry> entry = minioNode.locate(keyMapper.apply(central, junit));
                    assertFalse(entry.isPresent());

                    byte[] data = "Hello World!".getBytes(StandardCharsets.UTF_8);
                    Path temp = Files.createTempFile("mimir", "tmp");
                    Files.write(temp, data, StandardOpenOption.TRUNCATE_EXISTING);
                    Map<String, String> checksums = ChecksumAlgorithmHelper.calculate(
                            data,
                            Arrays.asList(new Sha1ChecksumAlgorithmFactory(), new Sha512ChecksumAlgorithmFactory()));
                    minioNode.store(keyMapper.apply(central, junit), temp, Map.of(), checksums);

                    entry = minioNode.locate(keyMapper.apply(central, junit));
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
                }
            } finally {
                container.stop();
            }
        }
    }
}
