/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.testing;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class MimirInfuserTest {
    @Test
    void smoke(@TempDir Path tmpDir) throws IOException {
        Path oldRoot = tmpDir.resolve("oldRoot");
        Path mimirBasedir = oldRoot.resolve(".mimir");
        Path extensionsXml = oldRoot.resolve(".m2").resolve("extensions.xml");
        Files.createDirectories(extensionsXml.getParent());
        Files.writeString(
                extensionsXml,
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + "<extensions>\n"
                        + "    <extension>\n"
                        + "        <groupId>eu.maveniverse.maven.mimir</groupId>\n"
                        + "        <artifactId>extension3</artifactId>\n"
                        + "        <version>0.8.0</version>\n"
                        + "    </extension>\n"
                        + "</extensions>\n");

        Path newRoot = tmpDir.resolve("newRoot");
        System.setProperty("user.home", oldRoot.toString());
        MimirInfuser.infuse(newRoot);
        Properties props = new Properties();
        try (InputStream is = Files.newInputStream(newRoot.resolve(".mimir").resolve("session.properties"))) {
            props.load(is);
        }
        assertEquals(3, props.size());
        assertEquals(mimirBasedir.toString(), props.getProperty("mimir.daemon.basedir"));
        assertEquals("false", props.getProperty("mimir.daemon.autostart"));
        assertEquals("false", props.getProperty("mimir.daemon.autoupdate"));
    }
}
