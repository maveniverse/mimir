/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.it.preseed.infuser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;
import eu.maveniverse.maven.mimir.testing.MimirInfuser;

public class InfusingTest {
    @Test
    void doInfuseLocalRepository() throws IOException {
        MimirInfuser.preseedItselfIntoInnerLocalRepository(Paths.get("target/local-repo"));
    }
}
