package eu.maveniverse.maven.mimir.jgroups;

import eu.maveniverse.maven.mimir.shared.CacheEntry;
import eu.maveniverse.maven.mimir.shared.CacheKey;
import eu.maveniverse.maven.mimir.shared.impl.LocalNodeFactoryImpl;
import eu.maveniverse.maven.mimir.shared.node.LocalCacheEntry;
import eu.maveniverse.maven.mimir.shared.node.LocalNode;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.blocks.MessageDispatcher;
import org.jgroups.blocks.RequestHandler;
import org.jgroups.blocks.Response;
import org.jgroups.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JGroupsPublisher implements RequestHandler, AutoCloseable {
    public static void main(String... args) throws Exception {
        Logger logger = LoggerFactory.getLogger(JGroupsPublisher.class);

        Path basedir;
        String nodeName;
        if (args.length > 0) {
            basedir = Paths.get(args[0]).toAbsolutePath();
        } else {
            basedir =
                    Paths.get(System.getProperty("user.home")).resolve(".mimir").resolve("local");
        }
        if (args.length > 1) {
            nodeName = args[1];
        } else {
            nodeName = InetAddress.getLocalHost().getHostName();
        }

        HashMap<String, Object> config = new HashMap<>();
        config.put("mimir.basedir", basedir.toString());
        config.put("mimir.local.name", nodeName);
        LocalNode localNode = new LocalNodeFactoryImpl().createLocalNode(config);
        JGroupsPublisher publisher = new JGroupsPublisher(
                localNode,
                new JChannel("udp-new.xml")
                        .name(nodeName)
                        .setDiscardOwnMessages(true)
                        .connect("mimir-jgroups"));

        logger.info("");
        logger.info("JGroupsPublisher started (Ctrl+C to exit)");
        logger.info("Publishing:");
        logger.info("* {} ({})", localNode.id(), localNode.basedir());
        try {
            new CountDownLatch(1).await(); // this is merely to get interrupt
        } catch (InterruptedException e) {
            publisher.close();
        }
    }

    public static final String CMD_LOOKUP = "LOOKUP ";
    public static final String RSP_LOOKUP_OK = "OK ";
    public static final String RSP_LOOKUP_KO = "KO ";

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final LocalNode localNode;
    private final JChannel channel;
    private final MessageDispatcher dispatcher;
    private final ServerSocket serverSocket;
    private final ConcurrentHashMap<String, LocalCacheEntry> tx;
    private final ExecutorService executor;
    private final Thread serverThread;

    public JGroupsPublisher(LocalNode localNode, JChannel channel) throws IOException {
        this.localNode = localNode;
        this.channel = channel;
        this.dispatcher = new MessageDispatcher(channel, this);
        this.serverSocket = new ServerSocket(0);
        this.tx = new ConcurrentHashMap<>();
        this.executor = Executors.newFixedThreadPool(6);

        this.serverThread = new Thread(() -> {
            try {
                while (true) {
                    final Socket accepted = serverSocket.accept();
                    executor.submit(() -> {
                        try (Socket socket = accepted) {
                            byte[] buf = socket.getInputStream().readNBytes(36);
                            OutputStream out = socket.getOutputStream();
                            if (buf.length == 36) {
                                String txid = new String(buf, StandardCharsets.UTF_8);
                                LocalCacheEntry cacheEntry = tx.remove(txid);
                                if (cacheEntry != null) {
                                    logger.info("SERVER HIT: {} to {}", txid, socket.getRemoteSocketAddress());
                                    cacheEntry.getInputStream().transferTo(out);
                                } else {
                                    logger.info("SERVER MISS: {} to {}", txid, socket.getRemoteSocketAddress());
                                }
                            }
                            out.flush();
                        } catch (Exception e) {
                            logger.error("Error while serving a client", e);
                        }
                    });
                }
            } catch (Exception e) {
                logger.error("Error while accepting client connection", e);
                try {
                    close();
                } catch (Exception ignore) {
                }
            }
        });
        serverThread.setDaemon(true);
        serverThread.start();
    }

    @Override
    public void close() throws Exception {
        dispatcher.close();
        channel.close();
        executor.shutdown();
        serverSocket.close();
    }

    @Override
    public Object handle(Message msg) throws Exception {
        AtomicReference<Object> resp = new AtomicReference<>(null);
        AtomicBoolean respIsException = new AtomicBoolean(false);
        Response response = new Response() {
            @Override
            public void send(Object reply, boolean is_exception) {
                resp.set(reply);
                respIsException.set(is_exception);
            }

            @Override
            public void send(Message reply, boolean is_exception) {
                resp.set(reply);
                respIsException.set(is_exception);
            }
        };
        handle(msg, response);
        if (respIsException.get()) {
            throw new IllegalArgumentException(String.valueOf(resp.get()));
        }
        return resp.get();
    }

    @Override
    public void handle(Message msg, Response response) throws Exception {
        String cmd = msg.getObject();
        if (cmd != null && cmd.startsWith(CMD_LOOKUP)) {
            String keyString = cmd.substring(CMD_LOOKUP.length());
            CacheKey key = CacheKey.fromKeyString(keyString);
            Optional<CacheEntry> entry = localNode.locate(key);
            if (entry.isPresent()) {
                String txid = UUID.randomUUID().toString();
                tx.put(txid, (LocalCacheEntry) entry.orElseThrow());
                response.send(
                        RSP_LOOKUP_OK + serverSocket.getInetAddress().getHostAddress() + ":"
                                + serverSocket.getLocalPort() + " " + txid,
                        false);
                logger.info("LOOKUP OK: {} as {}", keyString, txid);
                return;
            } else {
                response.send(RSP_LOOKUP_KO, false);
                logger.info("LOOKUP KO: {}", keyString);
                return;
            }
        }
        logger.info("UNKNOWN COMMAND: {}", cmd);
        response.send("Unknown command", true);
    }
}
