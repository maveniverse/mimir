/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.testing;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.mimir.daemon.protocol.Handle;
import eu.maveniverse.maven.mimir.daemon.protocol.Request;
import eu.maveniverse.maven.mimir.daemon.protocol.Response;
import eu.maveniverse.maven.mimir.daemon.protocol.Session;
import eu.maveniverse.maven.mimir.shared.SessionConfig;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

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
    public static final AtomicReference<String> MIMIR_VERSION = new AtomicReference<>("UNKNOWN");

    static {
        final Properties props = new Properties();
        try (InputStream is = MimirInfuser.class
                .getClassLoader()
                .getResourceAsStream("META-INF/maven/" + MIMIR_EXTENSION_GROUP_ID + "/testing/pom.properties")) {
            if (is != null) {
                props.load(is);
            }
        } catch (IOException e) {
            // fall through
        }
        if (!props.isEmpty()) {
            MIMIR_VERSION.set(props.getProperty("version"));
        }
    }

    /**
     * Preseeds the Mimir extension into given inner user home.
     *
     * @since 0.10.1
     */
    public static void preseedItselfIntoInnerUserHome(Path innerUserHome) throws IOException {
        requireNonNull(innerUserHome);
        preseedItselfIntoInnerLocalRepository(innerUserHome.resolve(".m2").resolve("repository"));
    }

    /**
     * Preseeds the Mimir extension into given Maven local repository.
     *
     * @since 0.10.1
     */
    public static void preseedItselfIntoInnerLocalRepository(Path innerLocalRepository) throws IOException {
        requireNonNull(innerLocalRepository);
        SessionConfig sessionConfig = SessionConfig.defaults().build();
        try (Handle.ClientHandle client =
                Handle.clientDomainSocket(sessionConfig.basedir().resolve(Handle.DEFAULT_SOCKET_PATH))) {
            Response response;
            Map<String, String> session;
            try (Handle handle = client.getHandle()) {
                HashMap<String, String> clientData = new HashMap<>();
                clientData.put(
                        Session.NODE_PID, Long.toString(ProcessHandle.current().pid()));
                clientData.put(Session.NODE_VERSION, MIMIR_VERSION.get());
                handle.writeRequest(Request.hello(clientData));
                response = handle.readResponse();
                if (Response.STATUS_KO.equals(response.status())) {
                    throw new IOException("KO: " + response.data());
                }
                session = response.session();
            }
            try (Handle handle = client.getHandle()) {
                handle.writeRequest(Request.preseedItself(
                        session, innerLocalRepository.toAbsolutePath().toString(), Map.of()));
                response = handle.readResponse();
                if (Response.STATUS_KO.equals(response.status())) {
                    throw new IOException("KO: " + response.data());
                }
            }
            try (Handle handle = client.getHandle()) {
                handle.writeRequest(Request.bye(session, false));
                response = handle.readResponse();
                if (Response.STATUS_KO.equals(response.status())) {
                    throw new IOException("KO: " + response.data());
                }
            }
        }
    }

    /**
     * Generates {@code extension.xml} contents for Mimir using given version. Usable when Mimir is the only
     * extension to be used.
     *
     * @since 0.10.0
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
     *
     * @since 0.10.0
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
     * Returns {@code true} if "outer" UW extensions are detected to contain Mimir.
     *
     * @since 0.10.0
     */
    public static boolean isMimirPresentUW() throws IOException {
        return isMimirPresentInExtensionsXml(
                Path.of(System.getProperty("user.home")).resolve(".m2").resolve("extensions.xml"));
    }

    /**
     * Returns {@code true} if "outer" PW extensions are detected to contain Mimir.
     *
     * @since 0.10.0
     */
    public static boolean isMimirPresentPW(Path outerSessionRoot) throws IOException {
        return isMimirPresentInExtensionsXml(outerSessionRoot.resolve(".mvn").resolve("extensions.xml"));
    }

    /**
     * This method assumes:
     * <ul>
     *     <li>Java System Property {@code user.hom} contains "outer" user home</li>
     *     <li>passed in {@link Path} represents the "inner" user home</li>
     *     <li>inner build want to use user wide extensions.xml</li>
     * </ul>
     * This method unconditionally writes out UW extensions.xml with Mimir.
     *
     * @since 0.10.1
     */
    public static void doInfuseUW(Path innerUserHome) throws IOException {
        doInfuseUW(MIMIR_VERSION.get(), innerUserHome);
    }

    /**
     * This method assumes:
     * <ul>
     *     <li>Java System Property {@code user.hom} contains "outer" user home</li>
     *     <li>passed in {@link Path} represents the "inner" user home</li>
     *     <li>inner build want to use user wide extensions.xml</li>
     * </ul>
     * This method unconditionally writes out UW extensions.xml with Mimir.
     *
     * @since 0.10.0
     */
    public static void doInfuseUW(String mimirVersion, Path innerUserHome) throws IOException {
        Path outerUserHome = Path.of(System.getProperty("user.home"));
        Path innerUWExtensions = innerUserHome.resolve(".m2").resolve("extensions.xml");
        Files.createDirectories(innerUWExtensions.getParent());
        Files.writeString(innerUWExtensions, extensionsXml(mimirVersion));
        infuseMimirSession(outerUserHome, innerUserHome);
    }

    /**
     * This method assumes:
     * <ul>
     *     <li>Java System Property {@code user.hom} contains "outer" user home</li>
     *     <li>passed in {@link Path} represents the "inner" session root and user home</li>
     *     <li>inner build want to use project wide extensions.xml</li>
     * </ul>
     * This method unconditionally writes out PW extensions.xml with Mimir.
     *
     * @since 0.10.0
     */
    public static void doInfusePW(Path innerSessionRoot, Path innerUserHome) throws IOException {
        doInfusePW(MIMIR_VERSION.get(), innerSessionRoot, innerUserHome);
    }

    /**
     * This method assumes:
     * <ul>
     *     <li>Java System Property {@code user.hom} contains "outer" user home</li>
     *     <li>passed in {@link Path} represents the "inner" session root and user home</li>
     *     <li>inner build want to use project wide extensions.xml</li>
     * </ul>
     * This method unconditionally writes out PW extensions.xml with Mimir.
     *
     * @since 0.10.0
     */
    public static void doInfusePW(String mimirVersion, Path innerSessionRoot, Path innerUserHome) throws IOException {
        Path outerUserHome = Path.of(System.getProperty("user.home"));
        Path innerPWExtensions = innerSessionRoot.resolve(".mvn").resolve("extensions.xml");
        Files.createDirectories(innerPWExtensions.getParent());
        Files.writeString(innerPWExtensions, extensionsXml(mimirVersion));
        infuseMimirSession(outerUserHome, innerUserHome);
    }

    /**
     * This method assumes:
     * <ul>
     *     <li>outer build uses user wide extensions.xml</li>
     *     <li>Java System Property {@code user.hom} contains "real" user home</li>
     *     <li>passed in {@link Path} represents the chroot-ed user home</li>
     *     <li>inner build want to use user wide extensions.xml</li>
     * </ul>
     * Infuses only IF outer UW detected to use Mimir, and returns {@code true}.
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
     *     <li>outer build uses project wide extensions.xml</li>
     *     <li>Java System Property {@code user.hom} contains "real" user home</li>
     *     <li>passed in paths represent the outer Maven session root, inner Maven session root, and inner user home, respectively</li>
     *     <li>inner build want to use project wide extensions.xml</li>
     * </ul>
     * Infuses only IF outer PW detected to use Mimir, and returns {@code true}.
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
     * Infuses Mimir if detected in "outer" env.
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
