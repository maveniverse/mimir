package eu.maveniverse.maven.mimir.shared.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import eu.maveniverse.maven.mimir.shared.impl.checksum.ChecksumInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.eclipse.aether.internal.impl.checksum.Md5ChecksumAlgorithmFactory;
import org.eclipse.aether.internal.impl.checksum.Sha1ChecksumAlgorithmFactory;
import org.eclipse.aether.spi.connector.checksum.ChecksumAlgorithmHelper;
import org.junit.jupiter.api.Test;

public class ChecksumInputStreamTest {
    @Test
    void smoke() throws IOException {
        Map<String, String> checksums = new HashMap<>();
        try (InputStream inputStream = URI.create(
                        "https://repo.maven.apache.org/maven2/eu/maveniverse/maven/mimir/core/0.2.3/core-0.2.3.pom")
                .toURL()
                .openConnection()
                .getInputStream()) {
            Consumer<Map<String, String>> callback = c -> {
                checksums.putAll(c);
                System.out.println(checksums);

                String sha1 = c.get("SHA-1");
                if (!"838770fb0aba2df8af66b7b64b265e2918c21895".equals(sha1)) {
                    throw new UncheckedIOException(new IOException(
                            "Checksum sha1 check failed (expected: 838770fb0aba2df8af66b7b64b265e2918c21895 calculated: "
                                    + sha1 + ")"));
                }
                String md5 = c.get("MD5");
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
                            new Md5ChecksumAlgorithmFactory().getAlgorithm(),
                            Sha1ChecksumAlgorithmFactory.NAME,
                            new Sha1ChecksumAlgorithmFactory().getAlgorithm()),
                    callback)) {
                cis.read();
                cis.transferTo(OutputStream.nullOutputStream());
            }
        }
    }

    @Test
    void readWithOffset() throws IOException {
        byte[] data = new byte[10_000];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) (i * 31);
        }
        Sha1ChecksumAlgorithmFactory sha1 = new Sha1ChecksumAlgorithmFactory();
        String expected = ChecksumAlgorithmHelper.calculate(data, List.of(sha1)).get(Sha1ChecksumAlgorithmFactory.NAME);
        AtomicReference<Map<String, String>> result = new AtomicReference<>();
        try (ChecksumInputStream cis = new ChecksumInputStream(
                new ByteArrayInputStream(data),
                Map.of(Sha1ChecksumAlgorithmFactory.NAME, sha1.getAlgorithm()),
                result::set)) {
            // offset 100: the first byte is not at index 0 of the buffer
            cis.readNBytes(new byte[data.length + 100], 100, data.length);
        }
        assertEquals(expected, result.get().get(Sha1ChecksumAlgorithmFactory.NAME));
    }

    @Test
    void skipIsHashed() throws IOException {
        byte[] data = "Hello World!".getBytes(StandardCharsets.UTF_8);
        AtomicReference<Map<String, String>> result = new AtomicReference<>();
        try (ChecksumInputStream cis = new ChecksumInputStream(
                new ByteArrayInputStream(data),
                Map.of(Sha1ChecksumAlgorithmFactory.NAME, new Sha1ChecksumAlgorithmFactory().getAlgorithm()),
                result::set)) {
            assertEquals(6, cis.skip(6));
            cis.transferTo(OutputStream.nullOutputStream());
        }
        assertEquals("2ef7bde608ce5404e97d5f042f95f89f1c232871", result.get().get(Sha1ChecksumAlgorithmFactory.NAME));
    }
}
