package eu.maveniverse.maven.mimir.jgroups;

import eu.maveniverse.maven.mimir.shared.CacheEntry;
import eu.maveniverse.maven.mimir.shared.CacheKey;
import eu.maveniverse.maven.mimir.shared.node.Node;
import java.io.IOException;
import java.util.Optional;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.ObjectMessage;

public class JGroupsNode implements Node {
    private final JChannel channel;

    public JGroupsNode(JChannel channel) {
        this.channel = channel;
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
        Message message = new ObjectMessage(null, "LOCATE " + CacheKey.toKeyString(key));
        return Optional.empty();
    }

    @Override
    public void close() throws Exception {
        channel.close();
    }
}
