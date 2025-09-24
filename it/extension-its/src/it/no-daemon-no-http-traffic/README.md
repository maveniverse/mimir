# No HTTP Traffic

This test primes Mimir cache in 1st pass, and then in 2nd pass makes use of it. 
Assertions are done against Mimir stats.

But, the IT uses "file" local node, does not download nor start daemon. Usable
when solo on single workstation.