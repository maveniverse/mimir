package eu.maveniverse.maven.mimir.node.bundle;

import static org.junit.jupiter.api.Assertions.assertFalse;

import eu.maveniverse.maven.mimir.shared.SessionConfig;
import eu.maveniverse.maven.mimir.shared.impl.naming.SimpleKeyResolverFactory;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class BundleNodeTest {
    @Test
    void noSessionNoBundle(@TempDir Path basedir) throws Exception {
        SessionConfig sessionConfig = SessionConfig.defaults().basedir(basedir).build();
        Optional<BundleNode> node = new BundleNodeFactory(
                        null, Map.of(SimpleKeyResolverFactory.NAME, new SimpleKeyResolverFactory()))
                .createLocalNode(sessionConfig);
        assertFalse(node.isPresent());
    }
}
