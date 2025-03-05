File firstLog = new File( basedir, 'first.log' )
assert firstLog.exists()

File secondLog = new File( basedir, 'second.log' )
assert secondLog.exists()

String log = secondLog.text

// Lets make strict assertion
// Explanation: cache was primed, so aside of "tail repo" everything else was located and retrieved, and
// nothing should be cached as cache was primed.
// Also Maven 3 vs 4 diff: they resolve differently
if (log.contains('Apache Maven 3.9.9')) {
    assert log.contains('[INFO] Mimir session closed (LOCATED=203/203 RETRIEVED=203 CACHED=0/0)')
} else if (log.contains('Apache Maven 4.0.0')) {
    assert log.contains('[INFO] Mimir session closed (LOCATED=195/195 RETRIEVED=195 CACHED=0/0)')
}
