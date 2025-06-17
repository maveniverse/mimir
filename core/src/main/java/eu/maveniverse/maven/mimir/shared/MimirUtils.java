/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.shared;

import static java.util.Objects.requireNonNull;

import java.util.Optional;
import java.util.function.Supplier;
import org.eclipse.aether.RepositorySystemSession;

public final class MimirUtils {
    private MimirUtils() {}

    public static Session lazyInit(RepositorySystemSession repositorySystemSession, Supplier<Session> sessionFactory) {
        requireNonNull(repositorySystemSession, "repositorySystemSession");
        requireNonNull(sessionFactory, "sessionFactory");
        Session session = (Session) repositorySystemSession.getData().get(Session.class.getName());
        if (session == null) {
            session = sessionFactory.get();
            repositorySystemSession.getData().set(Session.class.getName(), session);
        }
        return session;
    }

    public static Optional<Session> mayGetSession(RepositorySystemSession repositorySystemSession) {
        requireNonNull(repositorySystemSession, "repositorySystemSession");
        return Optional.ofNullable((Session) repositorySystemSession.getData().get(Session.class.getName()));
    }
}
