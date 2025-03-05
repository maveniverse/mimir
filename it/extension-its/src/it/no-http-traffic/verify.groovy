File buildLog = new File( basedir, 'second.log' )
assert buildLog.exists()

String log = buildLog.text
assert !log.contains ('RETRIEVED=0') // cache was primed, it must use it
assert log.contains('CACHED=0/0') // cache was primed, nothing should be stored
