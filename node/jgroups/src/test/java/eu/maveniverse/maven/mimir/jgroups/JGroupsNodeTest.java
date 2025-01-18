package eu.maveniverse.maven.mimir.jgroups;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import eu.maveniverse.maven.mimir.node.jgroups.JGroupsNode;
import eu.maveniverse.maven.mimir.shared.Config;
import eu.maveniverse.maven.mimir.shared.impl.LocalNodeConfig;
import eu.maveniverse.maven.mimir.shared.impl.LocalNodeImpl;
import eu.maveniverse.maven.mimir.shared.impl.SimpleKeyResolverFactory;
import eu.maveniverse.maven.mimir.shared.node.Entry;
import eu.maveniverse.maven.mimir.shared.node.LocalNode;
import java.net.InetAddress;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import org.eclipse.aether.internal.impl.checksum.Sha1ChecksumAlgorithmFactory;
import org.jgroups.JChannel;
import org.junit.jupiter.api.Test;

public class JGroupsNodeTest {

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

        LocalNodeConfig configOne =
                LocalNodeConfig.of("one", 0, one, Collections.singletonList("SHA-1"), SimpleKeyResolverFactory.NAME);
        LocalNode nodeOne = new LocalNodeImpl(
                configOne.name(),
                configOne.distance(),
                configOne.basedir(),
                new SimpleKeyResolverFactory().createKeyResolver(config),
                Map.of(Sha1ChecksumAlgorithmFactory.NAME, new Sha1ChecksumAlgorithmFactory()));
        LocalNodeConfig configTwo =
                LocalNodeConfig.of("two", 0, two, Collections.singletonList("SHA-1"), SimpleKeyResolverFactory.NAME);
        LocalNode nodeTwo = new LocalNodeImpl(
                configTwo.name(),
                configTwo.distance(),
                configTwo.basedir(),
                new SimpleKeyResolverFactory().createKeyResolver(config),
                Map.of(Sha1ChecksumAlgorithmFactory.NAME, new Sha1ChecksumAlgorithmFactory()));

        JChannel channelOne = new JChannel("udp-new.xml")
                .name(InetAddress.getLocalHost().getHostName() + "-one")
                .setDiscardOwnMessages(true)
                .connect("mimir-jgroups");
        JChannel channelTwo = new JChannel("udp-new.xml")
                .name(InetAddress.getLocalHost().getHostName() + "-two")
                .setDiscardOwnMessages(true)
                .connect("mimir-jgroups");
        Thread.sleep(1000);

        try (JGroupsNode publisher = new JGroupsNode(nodeOne, channelOne, true);
                JGroupsNode consumer = new JGroupsNode(nodeTwo, channelTwo, false); ) {
            URI key = URI.create("mimir:file:container:file.txt");
            Optional<Entry> entry = consumer.locate(key);
            assertTrue(entry.isPresent());

            Path tmpTarget = Files.createTempFile("tmp", ".tmp");
            entry.orElseThrow().transferTo(tmpTarget);

            assertEquals(content, Files.readString(tmpTarget));
        }
    }
}
