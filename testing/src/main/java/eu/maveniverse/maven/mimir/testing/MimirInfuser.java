/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.testing;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Properties;

/**
 * Class that sets up Mimir for embedded and chroot-ed maven executing tests; IF outer build uses Mimir as well
 * (like CI setup). This class helps to set up tests that are setting up user home directories for tests, and
 * documentation uses "outer" (build, or user home) for real, OS user home, and "inner" (build, or user home) for test
 * within environment created for test.
 * <p>
 * Using this class will transparently make "inner" Maven use "outer" Mimir daemon, contributing whatever it needs
 * to cache, and enjoying caching benefits.
 * <p>
 * Note: this class expects that current Java System Property {@code user.home} contain the "outer" (non chroot-ed)
 * user home (so invocation must happen from "outside"), and that the passed in path will be the chroot-ed new user home.
 * Also, it expects Mimir in default location. Feel free to copy-paste this class if any customization is needed.
 */
public final class MimirInfuser {
    public static final String MIMIR_EXTENSION_GROUP_ID = "eu.maveniverse.maven.mimir";
    public static final String MIMIR_EXTENSION_ARTIFACT_ID = "extension3";

    /**
     * Generates {@code extension.xml} contents for Mimir using given version. Usable when Mimir is the only
     * extension to be used.
     */
    public static String extensionsXml(String version) {
        requireNonNull(version);
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + System.lineSeparator()
                + "<extensions>" + System.lineSeparator()
                + "    <extension>" + System.lineSeparator()
                + "        <groupId>" + MIMIR_EXTENSION_GROUP_ID + "</groupId>" + System.lineSeparator()
                + "        <artifactId>" + MIMIR_EXTENSION_ARTIFACT_ID + "</artifactId>" + System.lineSeparator()
                + "        <version>" + version + "</version>" + System.lineSeparator()
                + "    </extension>" + System.lineSeparator()
                + "</extensions>" + System.lineSeparator();
    }

    /**
     * Returns {@code true} if {@code extensions.xml} contains Mimir extension.
     */
    public static boolean isMimirPresentInExtensionsXml(Path extensionXmlPath) throws IOException {
        if (Files.isRegularFile(extensionXmlPath)) {
            String extensionsXml = Files.readString(extensionXmlPath);
            return extensionsXml.contains("<groupId>" + MIMIR_EXTENSION_GROUP_ID + "</groupId>")
                    && extensionsXml.contains("<artifactId>" + MIMIR_EXTENSION_ARTIFACT_ID + "</artifactId>");
        }
        return false;
    }

    /**
     * This method assumes:
     * <ul>
     *     <li>user wide extensions.xml are used</li>
     *     <li>Java System Property {@code user.hom} contains "real" user home</li>
     *     <li>passed in {@link Path} represents the chroot-ed user home</li>
     * </ul>
     */
    public static boolean infuseUW(Path innerUserHome) throws IOException {
        requireNonNull(innerUserHome);
        Path outerUserHome = Path.of(System.getProperty("user.home"));
        Path outerUWExtensions = outerUserHome.resolve(".m2").resolve("extensions.xml");
        Path innerUWExtensions = innerUserHome.resolve(".m2").resolve("extensions.xml");
        return infuseMimir(outerUWExtensions, outerUserHome, innerUWExtensions, innerUserHome);
    }

    /**
     * This method assumes:
     * <ul>
     *     <li>project wide extensions.xml are used</li>
     *     <li>Java System Property {@code user.hom} contains "real" user home</li>
     *     <li>passed in paths represent the outer Maven session root, inner Maven session root, and inner user home, respectively</li>
     * </ul>
     */
    public static boolean infusePW(Path outerSessionRoot, Path innerSessionRoot, Path innerUserHome)
            throws IOException {
        requireNonNull(outerSessionRoot);
        requireNonNull(innerSessionRoot);
        requireNonNull(innerUserHome);
        Path outerUserHome = Path.of(System.getProperty("user.home"));
        Path outerPWExtensions = outerSessionRoot.resolve(".mvn").resolve("extensions.xml");
        Path innerPWExtensions = innerSessionRoot.resolve(".mvn").resolve("extensions.xml");
        return infuseMimir(outerPWExtensions, outerUserHome, innerPWExtensions, innerUserHome);
    }

    /**
     * Infuses Mimir.
     *
     * @param outerExtensions the location of "outer" {@code extensions.xml}
     * @param outerUserHome the location of "outer" user home
     * @param innerExtensions the location of "inner" {@code extensions.xml}
     * @param innerUserHome the location of "inner" user home
     */
    public static boolean infuseMimir(
            Path outerExtensions, Path outerUserHome, Path innerExtensions, Path innerUserHome) throws IOException {
        if (isMimirPresentInExtensionsXml(outerExtensions)) {
            if (!Files.isRegularFile(innerExtensions)) {
                Files.createDirectories(innerExtensions.getParent());
                Files.copy(outerExtensions, innerExtensions, StandardCopyOption.REPLACE_EXISTING);
                infuseMimirSession(outerUserHome, innerUserHome);
                return true;
            }
        }
        return false;
    }

    /**
     * Creates and writes out "inner" Mimir session properties pointing toward "outer" Mimir basedir.
     *
     * @param outerUserHome the "outer" Mimir basedir where daemon already runs.
     * @param innerUserHome the "inner" Mimir basedir that should use "outer" Mimir basedir.
     * @see #innerMimirSessionProperties(Path)
     */
    public static void infuseMimirSession(Path outerUserHome, Path innerUserHome) throws IOException {
        Properties properties = innerMimirSessionProperties(outerUserHome.resolve(".mimir"));
        Path innerMimirProperties = innerUserHome.resolve(".mimir").resolve("session.properties");
        Files.createDirectories(innerMimirProperties.getParent());
        try (OutputStream os = Files.newOutputStream(innerMimirProperties)) {
            properties.store(os, "Written by MimirInfuser");
        }
    }

    /**
     * Creates "inner" Mimir session properties pointing toward "outer" Mimir basedir. Also sets some "nice to have"
     * things like preventing autoupdate and autostart, to not lose time with these as outer build already should
     * use daemon.
     *
     * @param outerMimirBasedir the Mimir basedir where daemon already runs.
     */
    public static Properties innerMimirSessionProperties(Path outerMimirBasedir) {
        requireNonNull(outerMimirBasedir);
        Properties properties = new Properties();
        properties.setProperty("mimir.daemon.basedir", outerMimirBasedir.toString());
        properties.setProperty("mimir.daemon.autoupdate", "false");
        properties.setProperty("mimir.daemon.autostart", "false");
        return properties;
    }
}
