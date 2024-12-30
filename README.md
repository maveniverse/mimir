# Mímir

A system-wide (and wider) cache for Maven artifacts. Makes you love to nuke your local repository, as it 
contains only hard linked artifacts from Mímir (global) cache.

This is still a "rough proof of concept" that "works for me" (as my home is a huge btrfs volume, so hard-linking works).

Concept:
* "wraps" the actual connector being used by Resolver, provides "extra cache layer" between connector and resolver
* supports "local" (writeable) and "remote" (read only) Nodes, discovered on LAN (TBD)
* assumes getting content from LAN neighbor is faster than getting it from real remote
* may later provide "interfacing" to real MRMs as well (ie a Node may go for real MRM)
* this is the simplest proof of concept, but a node may be a separate Java process (thus exposing for others as "remote node")
* the node could offer locally and also remotely cached contents
* on local caching, "hard linking" should be used whenever possible (otherwise fallback to plain "copy")

Build requirements:
* Java 21
* Maven 3.9.9+

Runtime requirement:
* Java 8+
* Maven 3.6.3+

