package eu.maveniverse.maven.mimir.jgroups;

import eu.maveniverse.maven.mimir.shared.CacheEntry;
import eu.maveniverse.maven.mimir.shared.CacheKey;
import eu.maveniverse.maven.mimir.shared.node.LocalNode;
import eu.maveniverse.maven.mimir.shared.node.Node;
import java.io.IOException;
import java.util.Optional;

public class JGroupsNode implements Node {
    private final LocalNode localNode;

    public JGroupsNode(LocalNode localNode) {
        this.localNode = localNode;
    }

    @Override
    public String id() {
        return "jgroups";
    }

    @Override
    public int distance() {
        return 100;
    }

    @Override
    public Optional<CacheEntry> locate(CacheKey key) throws IOException {
        CacheEntry result = doLocate(key);
        if (result != null) {
            return Optional.of(localNode.store(key, result));
        }
        return Optional.empty();
    }

    @Override
    public void close() throws Exception {}

    private CacheEntry doLocate(CacheKey key) throws IOException {
        return null;
    }
}
