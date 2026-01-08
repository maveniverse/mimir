package eu.maveniverse.maven.mimir.node.ipfs;

import io.ipfs.api.IPFS;
import io.ipfs.api.NamedStreamable;
import io.ipfs.multihash.Multihash;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Disabled
public class IpfsNodeTest {
    private final Logger log = LoggerFactory.getLogger(getClass());

    @Test
    void smoke() throws IOException {
        IPFS ipfs = new IPFS("/ip4/127.0.0.1/tcp/5001");

        // publish
        ipfs.files.write(
                "/repo/maveniverse/pom.xml",
                new NamedStreamable.FileWrapper(Path.of("pom.xml").toFile()),
                true,
                true);
        Map stat = ipfs.files.stat("/repo/maveniverse/pom.xml");
        log.info("'{}'", stat);

        // consume
        Multihash filePointer = Multihash.fromBase58("QmfFPdDKqsLKMQAbU8MLdfZzw8T6JyRRGd2oQSVo5swN52");
        try (InputStream inputStream = ipfs.catStream(filePointer)) {
            long size = inputStream.transferTo(OutputStream.nullOutputStream());
            System.out.println(size);
        }
    }
}
