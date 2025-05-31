# Daemon Basedir

This test primes Mimir cache in 1st pass, and then in 2nd pass makes use of it. 
Assertions are done against Mimir stats.

The trick is first run "awakes" daemon, and then second run continues to use
it and finally "kills" it.

This test is prove we can easily "relocate" Mimir daemon basedir irrelevant of
actual "client side" of Mimir basedir.