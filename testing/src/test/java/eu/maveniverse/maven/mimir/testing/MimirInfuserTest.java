/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.testing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;

public class MimirInfuserTest {
    private static final String MIMIR_VERSION = System.getProperty("version.mimir");

    @Test
    void doInfuseUW(@TempDir(cleanup = CleanupMode.NEVER) Path tmpDir) throws IOException {
        Path outerHome = tmpDir.resolve("oldRoot");
        Path outerMimirBasedir = outerHome.resolve(".mimir");

        Path innerHome = tmpDir.resolve("newRoot");

        System.setProperty("user.home", outerHome.toString());
        MimirInfuser.doInfuseUW(MIMIR_VERSION, innerHome);

        Properties props = new Properties();
        try (InputStream is = Files.newInputStream(innerHome.resolve(".mimir").resolve("session.properties"))) {
            props.load(is);
        }
        assertEquals(3, props.size());
        assertEquals(outerMimirBasedir.toString(), props.getProperty("mimir.daemon.basedir"));
        assertEquals("false", props.getProperty("mimir.daemon.autostart"));
        assertEquals("false", props.getProperty("mimir.daemon.autoupdate"));

        assertEquals(
                MimirInfuser.extensionsXml(MIMIR_VERSION),
                Files.readString(innerHome.resolve(".m2").resolve("extensions.xml")));
    }

    @Test
    void doInfusePW(@TempDir(cleanup = CleanupMode.NEVER) Path tmpDir) throws IOException {
        Path outerHome = tmpDir.resolve("oldRoot");
        Path outerMimirBasedir = outerHome.resolve(".mimir");

        Path innerHome = tmpDir.resolve("newRoot");
        Path innerProject = innerHome.resolve("project");

        System.setProperty("user.home", outerHome.toString());
        MimirInfuser.doInfusePW(MIMIR_VERSION, innerProject, innerHome);

        Properties props = new Properties();
        try (InputStream is = Files.newInputStream(innerHome.resolve(".mimir").resolve("session.properties"))) {
            props.load(is);
        }
        assertEquals(3, props.size());
        assertEquals(outerMimirBasedir.toString(), props.getProperty("mimir.daemon.basedir"));
        assertEquals("false", props.getProperty("mimir.daemon.autostart"));
        assertEquals("false", props.getProperty("mimir.daemon.autoupdate"));

        assertEquals(
                MimirInfuser.extensionsXml(MIMIR_VERSION),
                Files.readString(innerProject.resolve(".mvn").resolve("extensions.xml")));
    }

    @Test
    void infuseUW(@TempDir(cleanup = CleanupMode.NEVER) Path tmpDir) throws IOException {
        Path outerHome = tmpDir.resolve("oldRoot");
        Path outerMimirBasedir = outerHome.resolve(".mimir");
        Path outerExtensionsXml = outerHome.resolve(".m2").resolve("extensions.xml");
        Files.createDirectories(outerExtensionsXml.getParent());
        Files.writeString(outerExtensionsXml, MimirInfuser.extensionsXml(MIMIR_VERSION));

        Path innerHome = tmpDir.resolve("newRoot");
        System.setProperty("user.home", outerHome.toString());
        assertTrue(MimirInfuser.infuseUW(innerHome));

        Properties props = new Properties();
        try (InputStream is = Files.newInputStream(innerHome.resolve(".mimir").resolve("session.properties"))) {
            props.load(is);
        }
        assertEquals(3, props.size());
        assertEquals(outerMimirBasedir.toString(), props.getProperty("mimir.daemon.basedir"));
        assertEquals("false", props.getProperty("mimir.daemon.autostart"));
        assertEquals("false", props.getProperty("mimir.daemon.autoupdate"));

        assertEquals(
                MimirInfuser.extensionsXml(MIMIR_VERSION),
                Files.readString(innerHome.resolve(".m2").resolve("extensions.xml")));
    }

    @Test
    void infusePW(@TempDir(cleanup = CleanupMode.NEVER) Path tmpDir) throws IOException {
        Path outerHome = tmpDir.resolve("oldRoot");
        Path outerMimirBasedir = outerHome.resolve(".mimir");
        Path outerProjectBasedir = outerHome.resolve("project");
        Path extensionsXml = outerProjectBasedir.resolve(".mvn").resolve("extensions.xml");
        Files.createDirectories(extensionsXml.getParent());
        Files.writeString(extensionsXml, MimirInfuser.extensionsXml(MIMIR_VERSION));

        Path innerHome = tmpDir.resolve("newRoot");
        Path innerProjectBasedir = innerHome.resolve("project");
        System.setProperty("user.home", outerHome.toString());
        assertTrue(MimirInfuser.infusePW(outerProjectBasedir, innerProjectBasedir, innerHome));

        Properties props = new Properties();
        try (InputStream is = Files.newInputStream(innerHome.resolve(".mimir").resolve("session.properties"))) {
            props.load(is);
        }
        assertEquals(3, props.size());
        assertEquals(outerMimirBasedir.toString(), props.getProperty("mimir.daemon.basedir"));
        assertEquals("false", props.getProperty("mimir.daemon.autostart"));
        assertEquals("false", props.getProperty("mimir.daemon.autoupdate"));

        assertEquals(
                MimirInfuser.extensionsXml(MIMIR_VERSION),
                Files.readString(innerProjectBasedir.resolve(".mvn").resolve("extensions.xml")));
    }
}
