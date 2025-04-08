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
// Explanation: cache was primed, so aside of "tail repo" everything else was located and retrieved, and
// nothing should be cached as cache was primed.
// Also Maven 3 vs 4 diff: they resolve differently
if (first.contains('Apache Maven 3.9.9')) {
    assert first.contains('[INFO] Mimir session closed (RETRIEVED=0 CACHED=216)') // both empty: mimir connector cached
    assert second.contains('[INFO] Mimir session closed (RETRIEVED=216 CACHED=0)') // mimir primed: all was retrieved
    assert third.contains('[INFO] Mimir session closed (RETRIEVED=0 CACHED=216)') // local repo primed: all was cached
} else if (first.contains('Apache Maven 4.0.0')) {
    assert first.contains('[INFO] Mimir session closed (RETRIEVED=0 CACHED=174)')
    assert second.contains('[INFO] Mimir session closed (RETRIEVED=174 CACHED=0)')
    assert third.contains('[INFO] Mimir session closed (RETRIEVED=0 CACHED=174)')
} else {
    throw new IllegalStateException("What Maven version is this?")
}
