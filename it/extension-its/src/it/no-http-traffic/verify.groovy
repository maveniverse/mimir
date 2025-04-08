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
assert first.contains('[INFO] Mimir session closed')
assert first.contains('RETRIEVED=0')
assert !first.contains('CACHED=0')

// second run: mimir is primed, local repo is empty: retrieved!=0 cached==0
assert second.contains('[INFO] Mimir session closed')
assert !second.contains('RETRIEVED=0')
assert second.contains('CACHED=0')

// third run: mimir is empty, local repo is primed: retrieved==0 cached!=0
assert third.contains('[INFO] Mimir session closed')
assert third.contains('RETRIEVED=0')
assert !third.contains('CACHED=0')
