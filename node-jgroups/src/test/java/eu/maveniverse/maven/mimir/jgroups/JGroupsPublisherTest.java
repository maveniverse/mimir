package eu.maveniverse.maven.mimir.jgroups;

import eu.maveniverse.maven.mimir.shared.impl.LocalNodeFactoryImpl;
import java.net.InetAddress;
import java.util.Collections;
import org.jgroups.JChannel;

public class JGroupsPublisherTest {
    public static void main(String... args) throws Exception {
        new JGroupsPublisher(
                new LocalNodeFactoryImpl().createLocalNode(Collections.emptyMap()),
                new JChannel("udp-new.xml")
                        .name(InetAddress.getLocalHost().getHostName())
                        .setDiscardOwnMessages(true)
                        .connect("mimir-jgroups"));
    }
}
