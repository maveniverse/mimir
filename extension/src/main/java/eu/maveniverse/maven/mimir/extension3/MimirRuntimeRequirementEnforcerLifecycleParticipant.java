/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.extension3;

import eu.maveniverse.maven.shared.extension.RuntimeRequirementEnforcerLifecycleParticipant;
import javax.inject.Named;
import javax.inject.Singleton;

/**
 * Lifecycle participant that enforces Njord requirements.
 * <p>
 * This class is intentionally self-contained and compiled with Java 8 to make it able to run on wide range of
 * Maven and Java versions, to report sane error for user why Njord refuses to work in their environment.
 */
@Singleton
@Named
public class MimirRuntimeRequirementEnforcerLifecycleParticipant
        extends RuntimeRequirementEnforcerLifecycleParticipant {
    private static final String MIMIR_MAVEN_REQUIREMENT = "[3.9,)";
    private static final String MIMIR_JAVA_REQUIREMENT = "[17,)";

    public MimirRuntimeRequirementEnforcerLifecycleParticipant() {
        super("Maveniverse Mimir", MIMIR_MAVEN_REQUIREMENT, MIMIR_JAVA_REQUIREMENT);
    }
}
