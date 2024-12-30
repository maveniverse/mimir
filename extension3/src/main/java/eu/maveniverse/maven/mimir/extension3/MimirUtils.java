/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.extension3;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.mimir.shared.Session;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.SessionData;

public final class MimirUtils {
    private MimirUtils() {}

    public static void seedSession(RepositorySystemSession repositorySystemSession, Session mimirSession) {
        requireNonNull(repositorySystemSession, "repositorySystemSession");
        requireNonNull(mimirSession, "mimirSession");
        SessionData data = repositorySystemSession.getData();
        if (data.get(Session.class) != null) {
            throw new IllegalStateException("Mimir session already created");
        }
        data.set(Session.class, mimirSession);
    }

    public static void closeSession(RepositorySystemSession repositorySystemSession) throws Exception {
        requireNonNull(repositorySystemSession, "repositorySystemSession");
        SessionData data = repositorySystemSession.getData();
        if (data.get(Session.class) == null) {
            throw new IllegalStateException("Mimir session not yet seeded");
        }
        ((Session) data.get(Session.class)).close();
    }

    public static Session requireSession(RepositorySystemSession repositorySystemSession) {
        requireNonNull(repositorySystemSession, "repositorySystemSession");
        SessionData data = repositorySystemSession.getData();
        if (data.get(Session.class) == null) {
            throw new IllegalStateException("Mimir session not yet seeded");
        }
        return (Session) data.get(Session.class);
    }
}
