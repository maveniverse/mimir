# Mímir

Note: this code is Proof of Concept, with a lot of To-Be-Done parts (like config).

Goal: A system-wide cache for Maven. Will make you to love to nuke your local repository. It adds a layer of
cache between resolver and transport. This implies it works irrelevant of location of your local repository 
and its kind (enhanced, split, whatever). You have one single "local" cache (by def in `~/.mimir/local) and it
is consulted before Maven would go remote. For now, only HTTP-ish release remote repositories are supported.
Another goal is to introduce "cache sharing" across LAN, so make Mimir able to share caches across LAN from several
workstations: basically if one workstation has the cache content, share it to neighbors if needed.

Concept:
* "wraps" the actual connector being used by Resolver, provides "extra cache layer" between connector and transport
* supports "local" (writeable) and "remote" (read only) Nodes, discovered on LAN or other means
* assumes getting content from Node (LAN neighbor) is faster than getting it from real remote (also saves outbound bandwidth)
* may later provide "interfacing" to real MRMs as well (ie a Node may go for real MRM)
* the node could offer locally and also remotely cached contents
* on local caching, "hard linking" should be used whenever possible (otherwise fallback to plain "copy") to avoid content duplication
* is irrelevant is project using it sits on "classic" or "split" or whatever local repository (as it is cache layer "above" local repository)
* current "simple proof of concept" is trivial, but later on, "local node" could spawn a daemon (a la mvnd) offering
  its cache content not only locally but also for remote nodes

Build requirements:
* Java 21
* Maven 3.9.9+

Runtime requirement for Core:
* Java 8+
* Maven 3.6.3+

Runtime requirement for node-jgroups:
* Java 17+
* Maven 3.6.3+
