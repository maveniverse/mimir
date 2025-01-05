package eu.maveniverse.maven.mimir.jgroups;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import eu.maveniverse.maven.mimir.shared.CacheEntry;
import eu.maveniverse.maven.mimir.shared.CacheKey;
import eu.maveniverse.maven.mimir.shared.Config;
import eu.maveniverse.maven.mimir.shared.impl.LocalNodeConfig;
import eu.maveniverse.maven.mimir.shared.impl.LocalNodeFactoryImpl;
import eu.maveniverse.maven.mimir.shared.impl.LocalNodeImpl;
import eu.maveniverse.maven.mimir.shared.node.LocalNode;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.jgroups.JChannel;
import org.junit.jupiter.api.Test;

public class JGroupsPublisherTest {

    @Test
    void smoke() throws Exception {
        Path one = Path.of("target/local/one");
        Files.createDirectories(one);
        Path two = Path.of("target/local/two");
        Files.createDirectories(two);

        Config config = Config.defaults().build();
        System.out.println(config.mimirVersion());

        Path contentPath = one.resolve("container").resolve("file.txt");
        String content = "Hello World!";
        Files.createDirectories(contentPath.getParent());
        Files.writeString(contentPath, content);

        LocalNode nodeOne = new LocalNodeImpl(LocalNodeConfig.of("one", 0, one));
        LocalNode nodeTwo = new LocalNodeImpl(LocalNodeConfig.of("two", 0, two));

        JChannel channelOne = new JChannel("udp-new.xml")
                .name(InetAddress.getLocalHost().getHostName() + "-one")
                .setDiscardOwnMessages(true)
                .connect("mimir-jgroups");
        JChannel channelTwo = new JChannel("udp-new.xml")
                .name(InetAddress.getLocalHost().getHostName() + "-two")
                .setDiscardOwnMessages(true)
                .connect("mimir-jgroups");
        Thread.sleep(1000);

        try (JGroupsPublisher publisher = new JGroupsPublisher(nodeOne, channelOne);
                JGroupsNode consumer = new JGroupsNode(nodeTwo, channelTwo); ) {
            CacheKey key = CacheKey.of("container", "file.txt");
            Optional<CacheEntry> entry = consumer.locate(key);
            assertTrue(entry.isPresent());

            Path tmpTarget = Files.createTempFile("tmp", ".tmp");
            entry.orElseThrow().transferTo(tmpTarget);

            assertEquals(content, Files.readString(tmpTarget));
        }
    }

    public static void main(String... args) throws Exception {
        new JGroupsPublisher(
                new LocalNodeFactoryImpl().createLocalNode(Config.defaults().build()),
                new JChannel("udp-new.xml")
                        .name(InetAddress.getLocalHost().getHostName())
                        .setDiscardOwnMessages(true)
                        .connect("mimir-jgroups"));
    }
}
