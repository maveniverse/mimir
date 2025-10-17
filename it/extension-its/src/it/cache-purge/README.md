# Cache Purge

Uses Mimir in two builds, but SLF4J API version differs between the two. Second build invokes
cache purge.

To make cache purge work, following is required:
* configure daemon for `mimir.file.exclusiveAccess=true`
* configure daemon for `mimir.file.cachePurge=ON_BEGIN`

In "exclusive access" the file now "owns" the storage, it is guaranteed only one node is accessing it. And 
cache purge when enabled, is applied. BOTH are needed, otherwise configuration failure is reported.

Note: file node may run in daemon or directly in embedded mode. In this test it runs in daemon, so
"exclusive access" with daemon does NOT limit client count connecting to daemon, it merely ensures
daemon alone is tampering with cache. OTOH, when daemon-less mode is used, "exclusive access" allows
only one process (the one running Maven with Mimir extension).