# Mímir

A system-wide (later a LAN or even organization wide) cache for Maven artifacts. Will make you love to nuke 
your local repository, as it contains only hard linked artifacts from Mímir (global) cache.

This is still a "rough proof of concept" that "works for me" (as my home is a huge btrfs volume, so hard-linking works).

Concept:
* "wraps" the actual connector being used by Resolver, provides "extra cache layer" between connector and transport
* supports "local" (writeable) and "remote" (read only) Nodes, discovered on LAN or other means (TBD)
* assumes getting content from Node (LAN neighbor) is faster than getting it from real remote (also saves bandwidth)
* may later provide "interfacing" to real MRMs as well (ie a Node may go for real MRM)
* the node could offer locally and also remotely cached contents
* on local caching, "hard linking" should be used whenever possible (otherwise fallback to plain "copy")
* is irrelevant is project using it uses "classic" or "split" or whatever local repository (as it is cache layer "above" local repository)
* current "simple proof of concept" is trivial, but later on, "local node" could spawn a daemon (a la mvnd) offering
  its cache content not only locally but also for remote nodes

Build requirements:
* Java 21
* Maven 3.9.9+

Runtime requirement:
* Java 8+
* Maven 3.6.3+

