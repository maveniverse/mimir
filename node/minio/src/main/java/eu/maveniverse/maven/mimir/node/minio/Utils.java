/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.node.minio;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public final class Utils {
    private Utils() {}

    public static Map<String, String> pushMap(Map<String, String> map) {
        HashMap<String, String> result = new HashMap<>(map.size());
        AtomicInteger counter = new AtomicInteger(0);
        map.forEach((k, v) -> result.put(String.valueOf(counter.incrementAndGet()), k + ";" + v));
        return result;
    }

    public static Map<String, String> popMap(Map<String, String> map) {
        HashMap<String, String> result = new HashMap<>(map.size());
        map.forEach((k, v) -> result.put(v.substring(0, v.indexOf(';')), v.substring(v.indexOf(';') + 1)));
        return result;
    }
}
