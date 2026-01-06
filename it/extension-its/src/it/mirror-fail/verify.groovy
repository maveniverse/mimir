/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
File buildLog = new File( basedir, 'build.log' )
assert buildLog.exists()
String build = buildLog.text

File firstLog = new File( basedir, 'first.log' )
assert firstLog.exists() : build
String first = firstLog.text

// Lets make strict assertion
// Also, consider Maven 3 vs 4 diff: they resolve differently; do not assert counts

// first run: both were empty: retrieved==0 cached!=0
assert first.contains('[INFO] Mimir session closed')
assert first.contains('RETRIEVED=0')
assert first.contains('CACHED=') && !first.contains('CACHED=0')

assert first.contains('Downloading from central: https://maven-central-eu.storage-download.googleapis.com/maven2/org/slf4j/slf4j-api-no-such-artifact/2.0.17/slf4j-api-no-such-artifact-2.0.17.pom')
assert first.contains('Downloading from central: https://maven-central-eu.storage-download.googleapis.com/maven2/org/junit/jupiter/junit-jupiter-api/5.12.1-no-such-version/junit-jupiter-api-5.12.1-no-such-version.pom')
assert first.contains('Downloading from central: https://repo.maven.apache.org/maven2/org/slf4j/slf4j-api-no-such-artifact/2.0.17/slf4j-api-no-such-artifact-2.0.17.pom')
assert first.contains('Downloading from central: https://repo.maven.apache.org/maven2/org/junit/jupiter/junit-jupiter-api/5.12.1-no-such-version/junit-jupiter-api-5.12.1-no-such-version.pom')

// the artifact from local-repo-main is resolved (is in local repo)
assert new File(basedir, 'first/group/artifact/1.0/artifact-1.0.pom').exists()