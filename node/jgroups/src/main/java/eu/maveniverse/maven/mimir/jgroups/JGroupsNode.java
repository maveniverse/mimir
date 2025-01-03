package eu.maveniverse.maven.mimir.jgroups;

import eu.maveniverse.maven.mimir.shared.CacheEntry;
import eu.maveniverse.maven.mimir.shared.CacheKey;
import eu.maveniverse.maven.mimir.shared.node.LocalNode;
import eu.maveniverse.maven.mimir.shared.node.Node;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import org.jgroups.JChannel;
import org.jgroups.ObjectMessage;
import org.jgroups.blocks.MessageDispatcher;
import org.jgroups.blocks.RequestOptions;
import org.jgroups.util.RspList;

public class JGroupsNode implements Node {
    private final LocalNode localNode;
    private final JChannel channel;
    private final MessageDispatcher messageDispatcher;

    public JGroupsNode(LocalNode localNode, JChannel channel) {
        this.localNode = localNode;
        this.channel = channel;
        this.messageDispatcher = new MessageDispatcher(channel);
    }

    @Override
    public String id() {
        return JGroupsNodeFactory.NAME;
    }

    @Override
    public int distance() {
        return 100;
    }

    @Override
    public Optional<CacheEntry> locate(CacheKey key) throws IOException {
        String cmd = JGroupsPublisher.CMD_LOOKUP + CacheKey.toKeyString(key);
        try {
            RspList<String> responses =
                    messageDispatcher.castMessage(null, new ObjectMessage(null, cmd), RequestOptions.SYNC());
            for (String response : responses.getResults()) {
                if (response != null && response.startsWith(JGroupsPublisher.RSP_LOOKUP_OK)) {
                    String body = response.substring(JGroupsPublisher.RSP_LOOKUP_OK.length());
                    String[] parts = body.split(" ");
                    if (parts.length == 2) {
                        int colon = parts[0].indexOf(':');
                        String host = parts[0].substring(0, colon);
                        int port = Integer.parseInt(parts[0].substring(colon + 1));
                        return Optional.of(localNode.store(key, new JGroupsCacheEntry(id(), host, port, parts[1])));
                    }
                }
            }
        } catch (Exception e) {
            throw new IOException("Failed to locate", e);
        }
        return Optional.empty();
    }

    @Override
    public void close() throws Exception {
        messageDispatcher.close();
        channel.close();
    }

    private record JGroupsCacheEntry(String origin, String host, int port, String txid) implements CacheEntry {
        @Override
        public void transferTo(Path file) throws IOException {
            try (Socket socket = new Socket(host, port)) {
                OutputStream os = socket.getOutputStream();
                os.write(txid.getBytes(StandardCharsets.UTF_8));
                os.flush();
                Files.copy(socket.getInputStream(), file, StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }
}
