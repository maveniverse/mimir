/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.shared;

import static java.util.Objects.requireNonNull;

import java.util.Objects;
import java.util.Optional;

public interface GACEV {
    String g();

    String a();

    Optional<String> c();

    String e();

    String v();

    static GACEV gav(String g, String a, String v) {
        return gaev(g, a, "jar", v);
    }

    static GACEV gaev(String g, String a, String e, String v) {
        return gacev(g, a, null, e, v);
    }

    static GACEV gacev(String g, String a, String c, String e, String v) {
        requireNonNull(g, "g");
        requireNonNull(a, "a");
        requireNonNull(e, "e");
        requireNonNull(v, "v");
        return new Impl(g, a, c == null || c.trim().isEmpty() ? null : c, e, v);
    }

    class Impl implements GACEV {
        private final String g;
        private final String a;
        private final String c;
        private final String e;
        private final String v;

        private Impl(String g, String a, String c, String e, String v) {
            this.g = g;
            this.a = a;
            this.c = c;
            this.e = e;
            this.v = v;
        }

        @Override
        public String g() {
            return g;
        }

        @Override
        public String a() {
            return a;
        }

        @Override
        public Optional<String> c() {
            return Optional.ofNullable(c);
        }

        @Override
        public String e() {
            return e;
        }

        @Override
        public String v() {
            return v;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Impl impl = (Impl) o;
            return Objects.equals(g, impl.g)
                    && Objects.equals(a, impl.a)
                    && Objects.equals(c, impl.c)
                    && Objects.equals(e, impl.e)
                    && Objects.equals(v, impl.v);
        }

        @Override
        public int hashCode() {
            return Objects.hash(g, a, c, e, v);
        }

        @Override
        public String toString() {
            return "Impl{" + "g='"
                    + g + '\'' + ", a='"
                    + a + '\'' + ", c='"
                    + c + '\'' + ", e='"
                    + e + '\'' + ", v='"
                    + v + '\'' + '}';
        }
    }
}
