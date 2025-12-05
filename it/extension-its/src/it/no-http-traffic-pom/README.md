# No HTTP Traffic w/ POM/extensions

Note: using Mimir as build extension is not recommended, and is also somewhat nonsense to do so.

This test primes Mimir cache in 1st pass, and then in 2nd pass makes use of it. 
Assertions are done against Mimir stats.