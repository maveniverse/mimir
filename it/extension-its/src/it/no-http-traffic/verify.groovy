File buildLog = new File( basedir, 'build.log' )
assert buildLog.exists()

String log = buildLog.text

// Lets make strict assertion
// Explanation: cache was primed, so aside of "tail repo" everything else was located and retrieved, and
// nothing should be cached as cache was primed.
// Also Maven 3 vs 4 diff: they resolve differently
if (log.contains('Apache Maven 3.9.9')) {
    assert log.contains('[INFO] Mimir session closed (RETRIEVED=203 CACHED=0)')
} else if (log.contains('Apache Maven 4.0.0')) {
    assert log.contains('[INFO] Mimir session closed (RETRIEVED=161 CACHED=0)')
} else {
    throw new IllegalStateException("What Maven version is this?")
}
