package eu.maveniverse.maven.mimir.node.ipfs;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.mimir.shared.impl.node.EntrySupport;
import eu.maveniverse.maven.mimir.shared.node.LocalEntry;
import eu.maveniverse.maven.mimir.shared.node.RemoteEntry;
import eu.maveniverse.maven.shared.core.fs.FileUtils;
import io.ipfs.api.IPFS;
import io.ipfs.multihash.Multihash;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class IpfsEntry extends EntrySupport implements LocalEntry, RemoteEntry {
    private final IPFS ipfs;
    private final Multihash multihash;

    public IpfsEntry(Map<String, String> metadata, Map<String, String> checksums, IPFS ipfs, Multihash multihash) {
        super(metadata, checksums);
        this.ipfs = requireNonNull(ipfs);
        this.multihash = requireNonNull(multihash);
    }

    @Override
    public void transferTo(Path file) throws IOException {
        handleContent(is -> {
            try (FileUtils.CollocatedTempFile tmp = FileUtils.newTempFile(file)) {
                Files.copy(is, tmp.getPath());
                tmp.move();
            }
        });
    }

    @Override
    public void handleContent(IOConsumer consumer) throws IOException {
        requireNonNull(consumer);
        try (InputStream inputStream = ipfs.catStream(multihash)) {
            consumer.accept(inputStream);
        }
    }
}
