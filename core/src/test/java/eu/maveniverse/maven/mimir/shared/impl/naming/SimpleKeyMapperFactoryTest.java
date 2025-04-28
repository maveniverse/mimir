package eu.maveniverse.maven.mimir.shared.impl.naming;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.RepositoryPolicy;
import org.junit.jupiter.api.Test;

public class SimpleKeyMapperFactoryTest {
    private final Artifact irrelevant = new DefaultArtifact("irrelevant:irrelevant:1.0");
    private final RemoteRepository central1 = new RemoteRepository.Builder(
                    "central", "default", "https://repo.maven.apache.org/maven2/")
            .setSnapshotPolicy(new RepositoryPolicy(false, "", ""))
            .build();
    private final RemoteRepository central2 = new RemoteRepository.Builder(
                    "central", "default", "https://repo1.maven.org/maven2")
            .setSnapshotPolicy(new RepositoryPolicy(false, "", ""))
            .build();
    private final RemoteRepository central3 = new RemoteRepository.Builder(
                    "central", "default", "https://maven-central.storage-download.googleapis.com/maven2/")
            .setSnapshotPolicy(new RepositoryPolicy(false, "", ""))
            .build();
    private final RemoteRepository central4 = new RemoteRepository.Builder(
                    "central", "default", "https://mymirror.fluke/maven2")
            .setSnapshotPolicy(new RepositoryPolicy(false, "", ""))
            .build();

    private final RemoteRepository anyrepo = new RemoteRepository.Builder(
                    "anyrepo", "default", "https://anyrepo.fluke/maven2")
            .setSnapshotPolicy(new RepositoryPolicy(false, "", ""))
            .build();

    @Test
    void smoke() {
        SimpleKeyMapperFactory.SimpleKeyMapper simpleKeyMapperFactory = new SimpleKeyMapperFactory.SimpleKeyMapper();

        assertEquals(
                "mimir:artifact:central:irrelevant:irrelevant:jar:1.0",
                simpleKeyMapperFactory.apply(central1, irrelevant).toASCIIString());
        assertEquals(
                "mimir:artifact:central:irrelevant:irrelevant:jar:1.0",
                simpleKeyMapperFactory.apply(central2, irrelevant).toASCIIString());
        assertEquals(
                "mimir:artifact:central:irrelevant:irrelevant:jar:1.0",
                simpleKeyMapperFactory.apply(central3, irrelevant).toASCIIString());

        assertEquals(
                "mimir:artifact:central-6e2d33a82b847b8df5aa72cf9b61f4d4d200b934:irrelevant:irrelevant:jar:1.0",
                simpleKeyMapperFactory.apply(central4, irrelevant).toASCIIString());
        assertEquals(
                "mimir:artifact:anyrepo-33bc8f95c99bce8984cd0cd31b6f40ca49b4bb6a:irrelevant:irrelevant:jar:1.0",
                simpleKeyMapperFactory.apply(anyrepo, irrelevant).toASCIIString());
    }
}
