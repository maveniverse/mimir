File buildLog = new File( basedir, 'build.log' )
assert buildLog.exists()
String build = firstLog.text


File firstLog = new File( basedir, 'first.log' )
assert firstLog.exists()
String first = firstLog.text

File secondLog = new File( basedir, 'second.log' )
assert secondLog.exists()
String second = secondLog.text

File thirdLog = new File( basedir, 'third.log' )
assert thirdLog.exists()
String third = thirdLog.text

// Lets make strict assertion
// Also, consider Maven 3 vs 4 diff: they resolve differently; do not assert counts

// first run: both were empty: retrieved==0 cached!=0
assert first.contains('[INFO] Mimir session closed') : build
assert first.contains('RETRIEVED=0') : build
assert first.contains('CACHED=') && !first.contains('CACHED=0') : build

// second run: mimir is primed, local repo is empty: retrieved!=0 cached==0
assert second.contains('[INFO] Mimir session closed') : build
assert second.contains('RETRIEVED=') && !second.contains('RETRIEVED=0') : build
assert second.contains('CACHED=0') : build

// third run: mimir is empty, local repo is primed: retrieved==0 cached!=0
assert third.contains('[INFO] Mimir session closed') : build
assert third.contains('RETRIEVED=0') : build
assert third.contains('CACHED=') && !third.contains('CACHED=0') : build
