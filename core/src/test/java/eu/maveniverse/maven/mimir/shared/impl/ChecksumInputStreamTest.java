package eu.maveniverse.maven.mimir.shared.impl;

import eu.maveniverse.maven.mimir.shared.impl.checksum.ChecksumAlgorithmFactoryAdapter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import org.eclipse.aether.internal.impl.checksum.Md5ChecksumAlgorithmFactory;
import org.junit.jupiter.api.Test;

public class ChecksumInputStreamTest {
    @Test
    void smoke() throws IOException {
        Map<String, String> checksums = new HashMap<>();
        try (InputStream inputStream = new URL(
                        "https://repo.maven.apache.org/maven2/eu/maveniverse/maven/mimir/core/0.2.3/core-0.2.3.pom")
                .openConnection()
                .getInputStream()) {
            Consumer<Map<String, String>> callback = c -> {
                checksums.putAll(c);
                String md5 = c.get("MD5");
                System.out.println(checksums);
                if (!"fe750dfc0e49b9b46e7728b55890a3fa".equals(md5)) {
                    throw new UncheckedIOException(new IOException(
                            "Checksum md5 check failed (expected: fe750dfc0e49b9b46e7728b55890a3fa calculated: " + md5
                                    + ")"));
                }
            };
            try (ChecksumInputStream cis = new ChecksumInputStream(
                    inputStream,
                    Map.of(
                            Md5ChecksumAlgorithmFactory.NAME,
                            new ChecksumAlgorithmFactoryAdapter(new Md5ChecksumAlgorithmFactory()).getAlgorithm()),
                    callback)) {
                cis.read();
                cis.transferTo(OutputStream.nullOutputStream());
            }
        }
    }
}
