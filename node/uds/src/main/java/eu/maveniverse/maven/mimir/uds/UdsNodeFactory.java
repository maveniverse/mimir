package eu.maveniverse.maven.mimir.uds;

import eu.maveniverse.maven.mimir.shared.Config;
import eu.maveniverse.maven.mimir.shared.node.LocalNode;
import eu.maveniverse.maven.mimir.shared.node.Node;
import eu.maveniverse.maven.mimir.shared.node.NodeFactory;
import java.io.IOException;
import java.net.UnixDomainSocketAddress;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import javax.inject.Named;
import javax.inject.Singleton;

@Singleton
@Named(UdsNodeConfig.NAME)
public class UdsNodeFactory implements NodeFactory {
    @Override
    public Optional<Node> createNode(Config config, LocalNode localNode) throws IOException {
        SocketChannel socketChannel = createSocketChannel(config);
        if (socketChannel == null) {
            return Optional.empty();
        }
        return Optional.of(new UdsNode(socketChannel));
    }

    private SocketChannel createSocketChannel(Config config) throws IOException {
        UdsNodeConfig cfg = UdsNodeConfig.with(config);
        if (!cfg.enabled()) {
            return null;
        }
        if (!Files.exists(cfg.socketPath())) {
            if (cfg.autostart()) {
                startDaemon(cfg.socketPath(), config);
            }
        }
        return SocketChannel.open(UnixDomainSocketAddress.of(cfg.socketPath()));
    }

    private void startDaemon(Path socketPath, Config config) throws IOException {
        throw new IOException("Could not start UDS Daemon");
    }
}
