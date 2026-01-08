package eu.maveniverse.maven.mimir.node.ipfs;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.mimir.shared.impl.node.RemoteNodeSupport;
import eu.maveniverse.maven.mimir.shared.node.RemoteEntry;
import io.ipfs.api.IPFS;
import java.io.IOException;
import java.net.URI;
import java.util.Optional;

public class IpfsNode extends RemoteNodeSupport {
    private final String multiaddr;
    private final IPFS ipfs;

    public IpfsNode(String multiaddr) {
        super(IpfsNodeConfig.NAME, 5000);
        this.multiaddr = requireNonNull(multiaddr);
        this.ipfs = new IPFS(multiaddr);
    }

    @Override
    public Optional<? extends RemoteEntry> locate(URI key) throws IOException {
        return Optional.empty();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " (distance=" + distance + " multiaddr=" + multiaddr + ")";
    }

    @Override
    protected void doClose() {}
}
