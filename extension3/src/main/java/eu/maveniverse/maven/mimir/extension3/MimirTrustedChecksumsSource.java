package eu.maveniverse.maven.mimir.extension3;

import eu.maveniverse.maven.mimir.shared.CacheEntry;
import eu.maveniverse.maven.mimir.shared.Session;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.inject.Named;
import javax.inject.Singleton;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.repository.ArtifactRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.spi.checksums.TrustedChecksumsSource;
import org.eclipse.aether.spi.connector.checksum.ChecksumAlgorithmFactory;

@Singleton
@Named
public class MimirTrustedChecksumsSource implements TrustedChecksumsSource {
    @Override
    public Map<String, String> getTrustedArtifactChecksums(
            RepositorySystemSession session,
            Artifact artifact,
            ArtifactRepository artifactRepository,
            List<ChecksumAlgorithmFactory> checksumAlgorithmFactories) {
        if (artifactRepository instanceof RemoteRepository remoteRepository) {
            Optional<Session> sessionOptional = MimirUtils.mayGetSession(session);
            if (sessionOptional.isPresent()) {
                Session ms = sessionOptional.orElseThrow();
                if (ms.repositorySupported(remoteRepository) && ms.artifactSupported(artifact)) {
                    try {
                        Optional<CacheEntry> entry = ms.locate(remoteRepository, artifact);
                        if (entry.isPresent()) {
                            CacheEntry cacheEntry = entry.orElseThrow();
                            return Map.copyOf(cacheEntry.checksums());
                        }
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                }
            }
        }
        return Map.of();
    }

    @Override
    public Writer getTrustedArtifactChecksumsWriter(RepositorySystemSession session) {
        return null;
    }
}
