/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.node.daemon;

import static eu.maveniverse.maven.mimir.shared.impl.EntryUtils.mergeEntry;
import static eu.maveniverse.maven.mimir.shared.impl.EntryUtils.splitChecksums;
import static eu.maveniverse.maven.mimir.shared.impl.EntryUtils.splitMetadata;
import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.mimir.daemon.protocol.Handle;
import eu.maveniverse.maven.mimir.daemon.protocol.Request;
import eu.maveniverse.maven.mimir.daemon.protocol.Response;
import eu.maveniverse.maven.mimir.shared.impl.node.EntrySupport;
import eu.maveniverse.maven.mimir.shared.impl.node.NodeSupport;
import eu.maveniverse.maven.mimir.shared.node.LocalEntry;
import eu.maveniverse.maven.mimir.shared.node.LocalNode;
import eu.maveniverse.maven.shared.core.fs.FileUtils;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * This node is delegating all the work to daemon via Unix Domain Sockets.
 */
public class DaemonNode extends NodeSupport implements LocalNode {
    private final DaemonConfig daemonConfig;
    private final Handle.ClientHandle clientHandle;
    private final boolean autostop;
    private final Map<String, String> session;
    private final Map<String, String> daemonData;

    public DaemonNode(Map<String, String> clientData, DaemonConfig daemonConfig, boolean autostop) throws IOException {
        super(DaemonConfig.NAME);
        this.daemonConfig = requireNonNull(daemonConfig, "daemonConfig");
        this.clientHandle = Handle.clientDomainSocket(daemonConfig.socketPath());
        this.autostop = autostop;

        try (Handle handle = clientHandle.getHandle()) {
            handle.writeRequest(Request.hello(clientData));
            Response helloResponse = handle.readResponse();
            this.session = helloResponse.session();
            this.daemonData = helloResponse.data();
            logger.debug("Hello OK {}", helloResponse.data());
        }
    }

    @Override
    public List<String> checksumAlgorithms() throws IOException {
        try (Handle handle = clientHandle.getHandle()) {
            handle.writeRequest(Request.lsChecksums(session));
            return new ArrayList<>(handle.readResponse().data().keySet());
        }
    }

    @Override
    public Optional<DaemonEntry> locate(URI key) throws IOException {
        String keyString = key.toASCIIString();
        logger.debug("LOCATE '{}'", keyString);
        try (Handle handle = clientHandle.getHandle()) {
            handle.writeRequest(Request.locate(session, keyString));
            Response locateResponse = handle.readResponse();
            if (!locateResponse.data().isEmpty()) {
                return Optional.of(new DaemonEntry(
                        splitMetadata(locateResponse.data()), splitChecksums(locateResponse.data()), keyString));
            } else {
                return Optional.empty();
            }
        }
    }

    @Override
    public DaemonEntry store(URI key, Path file, Map<String, String> metadata, Map<String, String> checksums)
            throws IOException {
        String keyString = key.toASCIIString();
        String filePath = FileUtils.canonicalPath(file).toString();
        logger.debug("STORE PATH '{}' -> '{}'", keyString, filePath);
        try (Handle handle = clientHandle.getHandle()) {
            handle.writeRequest(Request.storePath(session, keyString, filePath, mergeEntry(metadata, checksums)));
            Response storePathResponse = handle.readResponse();
            if (!storePathResponse.data().isEmpty()) {
                return new DaemonEntry(
                        splitMetadata(storePathResponse.data()), splitChecksums(storePathResponse.data()), keyString);
            } else {
                // this theoretically can never happen: daemon will either store or fail
                throw new IOException("Failed to store " + filePath + "; check daemon logs");
            }
        }
    }

    @Override
    protected void doClose() throws IOException {
        try (clientHandle) {
            try (Handle handle = clientHandle.getHandle()) {
                if (autostop) {
                    logger.info("Daemon shutdown initiated");
                }
                handle.writeRequest(Request.bye(session, autostop));
                Response byeResponse = handle.readResponse();
                logger.debug("Bye OK {}", byeResponse.data());
            }
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " (basedir=" + daemonConfig.daemonBasedir() + "; daemonPID="
                + daemonData.getOrDefault("daemon.pid", "n/a") + "; daemonVersion="
                + daemonData.getOrDefault("daemon.version", "n/a") + ")";
    }

    public class DaemonEntry extends EntrySupport implements LocalEntry {
        private final String keyString;

        private DaemonEntry(Map<String, String> metadata, Map<String, String> checksums, String keyString) {
            super(metadata, checksums);
            this.keyString = keyString;
        }

        @Override
        public void transferTo(Path file) throws IOException {
            logger.debug("TRANSFER '{}'->'{}'", keyString, file);
            try (Handle handle = clientHandle.getHandle()) {
                handle.writeRequest(Request.transfer(
                        session, keyString, FileUtils.canonicalPath(file).toString()));
                handle.readResponse();
            }
        }
    }
}
