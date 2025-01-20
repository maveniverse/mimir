/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.shared.naming;

import static java.util.Objects.requireNonNull;

import java.util.Objects;

public interface Key {
    /**
     * The "container".
     */
    String container();

    /**
     * The "name".
     */
    String name();

    static Key of(final String container, final String name) {
        requireNonNull(container, "container");
        requireNonNull(name, "name");
        return new Impl(container, name);
    }

    class Impl implements Key {
        private final String container;
        private final String name;

        private Impl(String container, String name) {
            this.container = container;
            this.name = name;
        }

        @Override
        public String container() {
            return container;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Impl impl = (Impl) o;
            return Objects.equals(container, impl.container) && Objects.equals(name, impl.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(container, name);
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "{" + "container='" + container + '\'' + ", name='" + name + '\'' + '}';
        }
    }
}
