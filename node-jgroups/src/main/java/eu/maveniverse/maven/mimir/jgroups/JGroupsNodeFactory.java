package eu.maveniverse.maven.mimir.jgroups;

import eu.maveniverse.maven.mimir.shared.node.LocalNode;
import eu.maveniverse.maven.mimir.shared.node.Node;
import eu.maveniverse.maven.mimir.shared.node.NodeFactory;
import java.io.IOException;
import java.util.Map;
import javax.inject.Named;
import javax.inject.Singleton;

@Singleton
@Named(JGroupsNodeFactory.NAME)
public class JGroupsNodeFactory implements NodeFactory {
    public static final String NAME = "jgroups";

    @Override
    public Node createNode(Map<String, Object> config, LocalNode localNode) throws IOException {
        return new JGroupsNode(localNode);
    }
}
