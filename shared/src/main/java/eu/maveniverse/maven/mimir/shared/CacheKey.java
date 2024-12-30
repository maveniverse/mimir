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

public interface CacheKey {
    String bucket();

    GACEV name();

    static CacheKey of(final String bucket, final GACEV name) {
        requireNonNull(bucket, "bucket");
        requireNonNull(name, "name");
        return new Impl(bucket, name);
    }

    class Impl implements CacheKey {
        private final String bucket;
        private final GACEV name;

        private Impl(String bucket, GACEV name) {
            this.bucket = bucket;
            this.name = name;
        }

        @Override
        public String bucket() {
            return bucket;
        }

        @Override
        public GACEV name() {
            return name;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Impl impl = (Impl) o;
            return Objects.equals(bucket, impl.bucket) && Objects.equals(name, impl.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(bucket, name);
        }

        @Override
        public String toString() {
            return "Impl{" + "bucket='" + bucket + '\'' + ", name='" + name + '\'' + '}';
        }
    }
}