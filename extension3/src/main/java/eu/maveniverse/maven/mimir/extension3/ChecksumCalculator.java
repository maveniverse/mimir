package eu.maveniverse.maven.mimir.extension3;

import static java.util.Objects.requireNonNull;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import org.eclipse.aether.spi.connector.checksum.ChecksumAlgorithm;

public class ChecksumCalculator {
    private final Map<String, ChecksumAlgorithm> checksumAlgorithms;

    public ChecksumCalculator(Map<String, ChecksumAlgorithm> checksumAlgorithms) {
        this.checksumAlgorithms = requireNonNull(checksumAlgorithms, "checksumAlgorithms");
    }

    public void update(ByteBuffer buffer) {
        for (ChecksumAlgorithm checksum : checksumAlgorithms.values()) {
            ((Buffer) buffer).mark();
            checksum.update(buffer);
            ((Buffer) buffer).reset();
        }
    }

    public Map<String, String> getChecksums() {
        HashMap<String, String> result = new HashMap<>();
        for (Map.Entry<String, ChecksumAlgorithm> entry : checksumAlgorithms.entrySet()) {
            result.put(entry.getKey(), entry.getValue().checksum());
        }
        return result;
    }
}
