package eu.maveniverse.maven.mimir.shared.naming;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.util.function.BiFunction;

import eu.maveniverse.maven.mimir.shared.impl.naming.Artifacts;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.RepositoryPolicy;
import org.junit.jupiter.api.Test;

public class URIEncodersTest {
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
    void artifact() {
        BiFunction<RemoteRepository, Artifact, URI> simpleKeyMapperFactory = UriEncoders::artifactKeyBuilder;

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

    @Test
    void artifactToFileKeyConversion() {
        URI uri = UriEncoders.artifactKeyBuilder(central1, new DefaultArtifact("org.apache.maven:maven-core:3.9.12"));
        assertEquals("mimir:artifact:central:org.apache.maven:maven-core:jar:3.9.12", uri.toASCIIString());
        Keys.Key key = UriDecoders.apply(uri);
        assertInstanceOf(Keys.ArtifactKey.class, key);
        Keys.ArtifactKey akey = (Keys.ArtifactKey) key;
        assertEquals("central", akey.container());
        assertEquals(new DefaultArtifact("org.apache.maven:maven-core:jar:3.9.12"), akey.artifact());
        Keys.FileKey fkey = Keys.toFileKey(akey, Artifacts::artifactRepositoryPath);
        assertEquals("central", fkey.container());
        assertEquals("org/apache/maven/maven-core/3.9.12/maven-core-3.9.12.jar", fkey.path());
    }
}
