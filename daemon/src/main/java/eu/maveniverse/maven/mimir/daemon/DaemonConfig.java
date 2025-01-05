package eu.maveniverse.maven.mimir.daemon;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.mimir.shared.Config;
import java.nio.file.Path;

public class DaemonConfig {
    public static DaemonConfig with(Config config) {
        requireNonNull(config, "config");

        Path socketPath = config.basedir().resolve("uds-socket");
        if (config.effectiveProperties().containsKey("mimir.daemon.socketPath")) {
            socketPath =
                    Config.getCanonicalPath(Path.of(config.effectiveProperties().get("mimir.daemon.socketPath")));
        }
        return new DaemonConfig(socketPath);
    }

    public static DaemonConfig of(Path socketPath) {
        return new DaemonConfig(Config.getCanonicalPath(socketPath));
    }

    public static final String NAME = "daemon";

    private final Path socketPath;

    private DaemonConfig(Path socketPath) {
        this.socketPath = socketPath;
    }

    public Path socketPath() {
        return socketPath;
    }
}
