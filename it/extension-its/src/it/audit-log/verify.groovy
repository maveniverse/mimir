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
assert first.contains('[INFO] Mimir audit log: ')
assert first.contains('[INFO] Mimir audit log (project): ')
assert first.contains('[INFO] Mimir session closed')
assert first.contains('RETRIEVED=0')
assert first.contains('CACHED=') && !first.contains('CACHED=0')
