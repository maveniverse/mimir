package eu.maveniverse.maven.mimir.jgroups;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import eu.maveniverse.maven.mimir.node.file.FileNode;
import eu.maveniverse.maven.mimir.node.file.FileNodeConfig;
import eu.maveniverse.maven.mimir.node.jgroups.JGroupsNode;
import eu.maveniverse.maven.mimir.shared.SessionConfig;
import eu.maveniverse.maven.mimir.shared.impl.naming.SimpleKeyResolverFactory;
import eu.maveniverse.maven.mimir.shared.impl.publisher.ServerSocketPublisherFactory;
import eu.maveniverse.maven.mimir.shared.node.RemoteEntry;
import eu.maveniverse.maven.shared.core.fs.DirectoryLocker;
import java.net.InetAddress;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
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

        SessionConfig sessionConfig = SessionConfig.defaults().build();
        System.out.println(sessionConfig.mimirVersion());

        Path contentPath = one.resolve("container").resolve("file.txt");
        String content = "Hello World!";
        Files.createDirectories(contentPath.getParent());
        Files.writeString(contentPath, content);

        FileNodeConfig configOne = FileNodeConfig.of(
                one,
                one,
                true,
                Collections.singletonList("SHA-1"),
                SimpleKeyResolverFactory.NAME,
                false,
                FileNodeConfig.CachePurge.OFF);
        FileNode nodeOne = new FileNode(
                configOne.basedir(),
                configOne.baseLockDir(),
                configOne.mayLink(),
                configOne.exclusiveAccess(),
                configOne.cachePurge(),
                new SimpleKeyResolverFactory().createKeyResolver(sessionConfig),
                List.of(Sha1ChecksumAlgorithmFactory.NAME),
                Map.of(Sha1ChecksumAlgorithmFactory.NAME, new Sha1ChecksumAlgorithmFactory()),
                DirectoryLocker.INSTANCE);
        FileNodeConfig configTwo = FileNodeConfig.of(
                two,
                two,
                true,
                Collections.singletonList("SHA-1"),
                SimpleKeyResolverFactory.NAME,
                false,
                FileNodeConfig.CachePurge.OFF);
        FileNode nodeTwo = new FileNode(
                configTwo.basedir(),
                configTwo.baseLockDir(),
                configTwo.mayLink(),
                configTwo.exclusiveAccess(),
                configTwo.cachePurge(),
                new SimpleKeyResolverFactory().createKeyResolver(sessionConfig),
                List.of(Sha1ChecksumAlgorithmFactory.NAME),
                Map.of(Sha1ChecksumAlgorithmFactory.NAME, new Sha1ChecksumAlgorithmFactory()),
                DirectoryLocker.INSTANCE);

        String testGroup = UUID.randomUUID().toString();
        JChannel channelOne = new JChannel("udp-new.xml")
                .name(InetAddress.getLocalHost().getHostName() + "-one")
                .setDiscardOwnMessages(true);
        JChannel channelTwo = new JChannel("udp-new.xml")
                .name(InetAddress.getLocalHost().getHostName() + "-two")
                .setDiscardOwnMessages(true);
        Thread.sleep(1000);

        try (JGroupsNode publisher = new JGroupsNode(
                        testGroup,
                        channelOne,
                        new ServerSocketPublisherFactory().createPublisher(sessionConfig, nodeOne));
                JGroupsNode consumer = new JGroupsNode(testGroup, channelTwo)) {
            URI key = URI.create("mimir:file:container:file.txt");
            Optional<? extends RemoteEntry> entry = consumer.locate(key);
            assertTrue(entry.isPresent());
            Path tmpTarget = Files.createTempFile("tmp", ".tmp");
            entry.orElseThrow().handleContent(is -> Files.copy(is, tmpTarget, StandardCopyOption.REPLACE_EXISTING));

            assertEquals(content, Files.readString(tmpTarget));
        }
    }
}
