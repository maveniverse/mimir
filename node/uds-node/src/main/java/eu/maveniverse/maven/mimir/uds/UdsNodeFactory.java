package eu.maveniverse.maven.mimir.uds;

import eu.maveniverse.maven.mimir.shared.node.LocalNode;
import eu.maveniverse.maven.mimir.shared.node.Node;
import eu.maveniverse.maven.mimir.shared.node.NodeFactory;
import java.io.IOException;
import java.net.UnixDomainSocketAddress;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import javax.inject.Named;
import javax.inject.Singleton;

@Singleton
@Named(UdsNodeFactory.NAME)
public class UdsNodeFactory implements NodeFactory {
    public static final String NAME = "uds";

    @Override
    public Optional<Node> createNode(Map<String, Object> config, LocalNode localNode) throws IOException {
        try {
            SocketChannel socketChannel = createSocketChannel(config);
            if (socketChannel == null) {
                return Optional.empty();
            }
            return Optional.of(new UdsNode(socketChannel));
        } catch (Exception e) {
            throw new IOException("Failed to create UDS Socket", e);
        }
    }

    private SocketChannel createSocketChannel(Map<String, Object> config) throws IOException {
        Path socketPath;
        if (config.containsKey("mimir.uds.socket")) {
            socketPath = Path.of(config.get("mimir.uds.socket").toString());
        } else {
            socketPath = Path.of(System.getProperty("user.home"), ".mimir", "socket");
        }
        if (!Files.exists(socketPath)) {
            if (Boolean.parseBoolean((String) config.getOrDefault("mimir.uds.autostart", "true"))) {
                if (startDaemon(config)) {
                    return SocketChannel.open(UnixDomainSocketAddress.of(socketPath));
                }
            }
            return null;
        } else {
            return SocketChannel.open(UnixDomainSocketAddress.of(socketPath));
        }
    }

    private boolean startDaemon(Map<String, Object> config) {
        return false;
    }
}
