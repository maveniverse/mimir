# Cache Purge combined with Preseed

Uses Mimir in two builds, but SLF4J API version differs between the two. Second build invokes
cache purge.

To make cache purge work, following is required:
* configure daemon for `mimir.file.exclusiveAccess=true`
* configure daemon for `mimir.file.cachePurge=ON_BEGIN`

In "exclusive access" the file now "owns" the storage, it is guaranteed only one node is accessing it. And 
cache purge when enabled, is applied. BOTH are needed, otherwise configuration failure is reported.

NOTE: The scenario in this IT is quite convoluted, and hence, contains expectation that daemon is used
by single process! This test is here only to, well, test, and in real life you'd not do anything like 
this.