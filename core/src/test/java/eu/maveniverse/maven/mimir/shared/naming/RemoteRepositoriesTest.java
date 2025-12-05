package eu.maveniverse.maven.mimir.shared.naming;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.function.Predicate;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.RepositoryPolicy;
import org.junit.jupiter.api.Test;

public class RemoteRepositoriesTest {
    /**
     * Proper definition (policies set).
     */
    private final RemoteRepository central = new RemoteRepository.Builder(
                    "central", "default", "https://repo.maven.apache.org/maven2")
            .setReleasePolicy(new RepositoryPolicy(
                    true, RepositoryPolicy.UPDATE_POLICY_NEVER, RepositoryPolicy.CHECKSUM_POLICY_FAIL))
            .setSnapshotPolicy(new RepositoryPolicy(false, "", ""))
            .build();
    /**
     * Lazy definition (is wrong: both releases and snapshots are enabled).
     */
    private final RemoteRepository centralLazy =
            new RemoteRepository.Builder("central", "default", "https://repo.maven.apache.org/maven2").build();
    /**
     * Blocked.
     */
    private final RemoteRepository centralBlocked = new RemoteRepository.Builder(
                    "central", "default", "https://repo.maven.apache.org/maven2")
            .setReleasePolicy(new RepositoryPolicy(
                    true, RepositoryPolicy.UPDATE_POLICY_NEVER, RepositoryPolicy.CHECKSUM_POLICY_FAIL))
            .setSnapshotPolicy(new RepositoryPolicy(false, "", ""))
            .setBlocked(true)
            .build();
    /**
     * Is mirror of "real" central.
     */
    private final RemoteRepository centralMirrored = new RemoteRepository.Builder("central", "default", "https://mymrm")
            .setReleasePolicy(new RepositoryPolicy(
                    true, RepositoryPolicy.UPDATE_POLICY_NEVER, RepositoryPolicy.CHECKSUM_POLICY_FAIL))
            .setSnapshotPolicy(new RepositoryPolicy(false, "", ""))
            .setMirroredRepositories(Collections.singletonList(central))
            .build();
    /**
     * Proper definition but protocol is SFTP.
     */
    private final RemoteRepository centralSftp = new RemoteRepository.Builder("central", "default", "sftp://mymrm")
            .setReleasePolicy(new RepositoryPolicy(
                    true, RepositoryPolicy.UPDATE_POLICY_NEVER, RepositoryPolicy.CHECKSUM_POLICY_FAIL))
            .setSnapshotPolicy(new RepositoryPolicy(false, "", ""))
            .build();
    /**
     * Snapshots.
     */
    private final RemoteRepository snapshots = new RemoteRepository.Builder("snapshots", "default", "https://mymrm")
            .setReleasePolicy(new RepositoryPolicy(
                    false, RepositoryPolicy.UPDATE_POLICY_NEVER, RepositoryPolicy.CHECKSUM_POLICY_FAIL))
            .setSnapshotPolicy(new RepositoryPolicy(
                    true, RepositoryPolicy.UPDATE_POLICY_DAILY, RepositoryPolicy.CHECKSUM_POLICY_FAIL))
            .build();
    /**
     * Mixed.
     */
    private final RemoteRepository mixed = new RemoteRepository.Builder("snapshots", "default", "https://mymrm")
            .setReleasePolicy(new RepositoryPolicy(
                    true, RepositoryPolicy.UPDATE_POLICY_NEVER, RepositoryPolicy.CHECKSUM_POLICY_FAIL))
            .setSnapshotPolicy(new RepositoryPolicy(
                    true, RepositoryPolicy.UPDATE_POLICY_DAILY, RepositoryPolicy.CHECKSUM_POLICY_FAIL))
            .build();

    @Test
    void defValue() {
        Predicate<RemoteRepository> predicate = RemoteRepositories.repositoryPredicate(RemoteRepositories.DEFAULT);

        assertTrue(predicate.test(central));
        assertFalse(predicate.test(centralLazy));
        assertFalse(predicate.test(centralBlocked));
        assertFalse(predicate.test(centralMirrored));
        assertFalse(predicate.test(centralSftp));
        assertFalse(predicate.test(snapshots));
        assertFalse(predicate.test(mixed));
    }

    @Test
    void any() {
        Predicate<RemoteRepository> predicate = RemoteRepositories.repositoryPredicate(Collections.singletonList("*"));

        assertTrue(predicate.test(central));
        assertTrue(predicate.test(centralLazy));
        assertFalse(predicate.test(centralBlocked)); // blocked
        assertTrue(predicate.test(centralMirrored));
        assertTrue(predicate.test(centralSftp));
        assertFalse(predicate.test(snapshots)); // release policy disabled
        assertTrue(predicate.test(mixed));
    }

    @Test
    void anyHttpsOnly() {
        Predicate<RemoteRepository> predicate =
                RemoteRepositories.repositoryPredicate(Collections.singletonList("*(httpsOnly)"));

        assertTrue(predicate.test(central));
        assertTrue(predicate.test(centralLazy));
        assertFalse(predicate.test(centralBlocked)); // blocked
        assertTrue(predicate.test(centralMirrored));
        assertFalse(predicate.test(centralSftp)); // sftp
        assertFalse(predicate.test(snapshots)); // release policy disabled
        assertTrue(predicate.test(mixed));
    }

    @Test
    void anyDirectOnlyHttpsOnly() {
        Predicate<RemoteRepository> predicate =
                RemoteRepositories.repositoryPredicate(Collections.singletonList("*(directOnly,httpsOnly)"));

        assertTrue(predicate.test(central));
        assertTrue(predicate.test(centralLazy));
        assertFalse(predicate.test(centralBlocked)); // blocked
        assertFalse(predicate.test(centralMirrored)); // mirrored, not direct
        assertFalse(predicate.test(centralSftp)); // sftp
        assertFalse(predicate.test(snapshots)); // release policy disabled
        assertTrue(predicate.test(mixed));
    }

    @Test
    void byIdCentral() {
        Predicate<RemoteRepository> predicate =
                RemoteRepositories.repositoryPredicate(Collections.singletonList("central"));

        assertTrue(predicate.test(central));
        assertTrue(predicate.test(centralLazy));
        assertFalse(predicate.test(centralBlocked)); // blocked
        assertTrue(predicate.test(centralMirrored));
        assertTrue(predicate.test(centralSftp));
        assertFalse(predicate.test(snapshots)); // id != central
        assertFalse(predicate.test(mixed)); // id != central
    }

    @Test
    void byIdCentralHttpsOnly() {
        Predicate<RemoteRepository> predicate =
                RemoteRepositories.repositoryPredicate(Collections.singletonList("central(httpsOnly)"));

        assertTrue(predicate.test(central));
        assertTrue(predicate.test(centralLazy));
        assertFalse(predicate.test(centralBlocked)); // blocked
        assertTrue(predicate.test(centralMirrored));
        assertFalse(predicate.test(centralSftp)); // protocol != https
        assertFalse(predicate.test(snapshots)); // id != central
        assertFalse(predicate.test(mixed)); // id != central
    }

    @Test
    void byIdSnapshots() {
        Predicate<RemoteRepository> predicate =
                RemoteRepositories.repositoryPredicate(Collections.singletonList("snapshots"));

        assertFalse(predicate.test(central)); // id != snapshots
        assertFalse(predicate.test(centralLazy)); // id != snapshots
        assertFalse(predicate.test(centralBlocked)); // blocked
        assertFalse(predicate.test(centralMirrored)); // id != snapshots
        assertFalse(predicate.test(centralSftp)); // id != snapshots
        assertFalse(predicate.test(snapshots)); // release policy disabled
        assertTrue(predicate.test(mixed));
    }
}
