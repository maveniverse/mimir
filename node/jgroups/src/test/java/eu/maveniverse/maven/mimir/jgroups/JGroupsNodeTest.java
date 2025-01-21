package eu.maveniverse.maven.mimir.jgroups;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import eu.maveniverse.maven.mimir.node.file.FileNode;
import eu.maveniverse.maven.mimir.node.file.FileNodeConfig;
import eu.maveniverse.maven.mimir.node.jgroups.JGroupsNode;
import eu.maveniverse.maven.mimir.shared.Config;
import eu.maveniverse.maven.mimir.shared.impl.naming.SimpleKeyResolverFactory;
import eu.maveniverse.maven.mimir.shared.impl.publisher.ServerSocketPublisherFactory;
import eu.maveniverse.maven.mimir.shared.node.RemoteEntry;
import java.net.InetAddress;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.List;
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

        FileNodeConfig configOne =
                FileNodeConfig.of("one", one, Collections.singletonList("SHA-1"), SimpleKeyResolverFactory.NAME);
        FileNode nodeOne = new FileNode(
                configOne.name(),
                configOne.basedir(),
                new SimpleKeyResolverFactory().createKeyResolver(config),
                List.of(Sha1ChecksumAlgorithmFactory.NAME),
                Map.of(Sha1ChecksumAlgorithmFactory.NAME, new Sha1ChecksumAlgorithmFactory()));
        FileNodeConfig configTwo =
                FileNodeConfig.of("two", two, Collections.singletonList("SHA-1"), SimpleKeyResolverFactory.NAME);
        FileNode nodeTwo = new FileNode(
                configTwo.name(),
                configTwo.basedir(),
                new SimpleKeyResolverFactory().createKeyResolver(config),
                List.of(Sha1ChecksumAlgorithmFactory.NAME),
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

        try (JGroupsNode publisher = new JGroupsNode(nodeOne, channelOne, config, new ServerSocketPublisherFactory());
                JGroupsNode consumer = new JGroupsNode(nodeTwo, channelTwo)) {
            URI key = URI.create("mimir:file:container:file.txt");
            Optional<? extends RemoteEntry> entry = consumer.locate(key);
            assertTrue(entry.isPresent());
            Path tmpTarget = Files.createTempFile("tmp", ".tmp");
            entry.orElseThrow().handleContent(is -> Files.copy(is, tmpTarget, StandardCopyOption.REPLACE_EXISTING));

            assertEquals(content, Files.readString(tmpTarget));
        }
    }
}
