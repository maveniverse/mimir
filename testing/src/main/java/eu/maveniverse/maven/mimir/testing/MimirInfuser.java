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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Class that sets up Mimir for embedded and chroot-ed maven executing tests; IF outer build uses Mimir as well
 * (like CI setup).
 * <p>
 * Using this class will transparently use outer build used Mimir daemon, contributing whatever it needs to cache, and
 * enjoying caching benefits.
 * <p>
 * Note: this class expects that current property {@code user.home} be the "real" user home (so invocation must happen
 * outside), and that the passed in path to be the chroot-ed user home. Also, it expects Mimir in default location.
 * Feel free to copy-paste this class if any customization is needed.
 * For simplicity reason is user-wide extension used (Maven 4 feature), as for Maven 3 sessionRoot would need to be
 * passed all way to here.
 */
public final class MimirInfuser {
    public static void infuse(Path chrootUserHome) throws IOException {
        requireNonNull(chrootUserHome);
        Path realUserWideExtensions =
                Path.of(System.getProperty("user.home")).resolve(".m2").resolve("extensions.xml");
        if (Files.isRegularFile(realUserWideExtensions)) {
            String realUserWideExtensionsString = Files.readString(realUserWideExtensions);
            if (realUserWideExtensionsString.contains("<groupId>eu.maveniverse.maven.mimir</groupId>")
                    && realUserWideExtensionsString.contains("<artifactId>extension3</artifactId>")) {
                Path chrootUserWideExtensions = chrootUserHome.resolve(".m2").resolve("extensions.xml");
                // some tests do prepare project and user wide extensions; skip those for now
                if (!Files.isRegularFile(chrootUserWideExtensions)) {
                    Files.createDirectories(chrootUserWideExtensions.getParent());
                    Files.copy(realUserWideExtensions, chrootUserWideExtensions, StandardCopyOption.REPLACE_EXISTING);

                    Path chrootMimirProperties =
                            chrootUserHome.resolve(".mimir").resolve("session.properties");
                    Files.createDirectories(chrootMimirProperties.getParent());
                    String sessionProperties = "# Written by MimirInfuser\n"
                            + String.format("mimir.daemon.basedir=%s/.mimir\n", System.getProperty("user.home"))
                            + "mimir.daemon.autoupdate=false\n"
                            + "mimir.daemon.autostart=false";
                    Files.writeString(chrootMimirProperties, sessionProperties);
                }
            }
        }
    }
}
