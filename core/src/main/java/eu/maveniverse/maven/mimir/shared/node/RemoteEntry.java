/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.shared.node;

import java.io.IOException;
import java.io.InputStream;

/**
 * Remote entry, cannot use file system as it is "remote" to the caller, but it can accept a consumer that will get
 * the content as stream.
 */
public interface RemoteEntry extends Entry {
    /**
     * Consumer of remote entry content.
     */
    @FunctionalInterface
    interface IOConsumer {
        void accept(InputStream stream) throws IOException;
    }

    /**
     * Provides remote cache entry content as input stream to consumer.
     */
    void handleContent(IOConsumer consumer) throws IOException;
}
