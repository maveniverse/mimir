# Mímir

[![Maven Central](https://img.shields.io/maven-central/v/eu.maveniverse.maven.mimir/extension3.svg?label=Maven%20Central)](https://search.maven.org/artifact/eu.maveniverse.maven.mimir/extension3)

Note: this code is Proof of Concept, with a lot of To-Be-Done parts and intentionally simple as possible. For now
only "central" repository released artifacts are supported.

Goal: A system-wide cache for Maven. Will make you to love to nuke your local repository. It adds a layer of
cache between resolver and transport. This implies it works irrelevant of location of your local repository 
and its kind (enhanced, split, whatever). You have one single "local" system-wide cache (by def in `~/.mimir/local`) and it
is consulted before Maven would go remote. For now, only "central" release remote repository is supported.
This local cache can be used by all Maven builds running on same workstation.

Another goal is to introduce "local cache sharing" across LAN, so make Mimir able to share caches across LAN from several
workstations: basically if one workstation has the cache content, share it to neighbors. Daemon does this by default.

Concept:
* "wraps" the actual connector being used by Resolver, provides "extra cache layer" between connector and transport
* supports "local" (writeable) and "remote" (read only) Nodes, discovered on LAN or other means
* assumes getting content from Node (LAN neighbor) is faster than getting it from real remote (also saves outbound bandwidth)
* may later provide "interfacing" to real MRMs as well (ie a Node may go for real MRM)
* the node could offer locally and also remotely cached contents
* on local caching, "hard linking" should be used whenever possible (otherwise fallback to plain "copy") to avoid content duplication
* is irrelevant is project using it sits on "classic" or "split" or whatever local repository (as it is cache layer "above" local repository)
* RemoteRepository handling has a huge deal of TBDs, right now is almost trivial.

## To use it

With Maven 4 (latest master) create user-wide `~/.m2/extensions.xml` like this:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<extensions>
    <extension>
        <groupId>eu.maveniverse.maven.mimir</groupId>
        <artifactId>extension3</artifactId>
        <version>0.2.3</version>
    </extension>
</extensions>
```
Works with latest Maven 3 as well, but it does not support user-wide extensions, so file above should be placed under some project `.mvn/extensions.xml` file instead.

IF you have docker, tailscale (or just non-trivial networking setup), help JGroups to select your LAN interface: create `~/.mimir/daemon.properties` file like this:
```properties
mimir.jgroups.interface=match-address\:192.168.1.*
```
(use yor LAN IP address).

To make Mimir stop you nagging to auto-update, set `mimir.checkupdates` in `~/.mimir/mimir.properties`.

And just build with Maven...

Build requirements:
* Java 21
* Maven 3.9.9+

Runtime requirements:
* Java 17+
* Maven 3.6.3+

## High level design

Mimir defines "node" as basic building block. A node can be "remote" or "local". A "remote" node is getting content
from remote origin (as relative to current workstation). Mimir provides simple "publisher" abstraction, that allows
a "remote" node to become also a publisher (for remote nodes on other workstations).

A "local" node have one specialization, the "system" node. A system node is really a storage that is backed by
some storage tech (Mimir offers two system nodes: file system backed one and Minio backed one). Given system node
is local specialization, this means that Mimir offers two local nodes as well. And there is third local node (that
is NOT system node, as it is not backed by storage) the "daemon" node. Daemon node delegates all the node work
to a long-running daemon process, hence daemon node itself does not have storage.

In Maven process (with extension), a Mimir `Session` is created, that needs one `LocalNode`. This Mimir via session 
and connector integrates into Resolver.

If `file` local node is used, Mimir will provide "system-wide caching": no matter how many local repository copies you have,
you end up with one copy of each artifact on disk (in ideal case). Same stands for `minio` when used as local node.

If `daemon` local node is used, Mimir daemon needs to run (is auto started by default). Mimir Daemon process communicates
via Unix Domain Sockets (UDS) to `daemon` local node, and this node fully delegates all the work to Daemon process.
Given Daemon process is long living, it incorporates one `SystemNode` and several `RemoteNode`s. Daemon will consult
`SystemNode` first, and ask `RemoteNode`s for content if it is not present locally. If remote node has it, 
write-through caching happens via `SystemNode`. Similarly, daemon contained `RemoteNodes` will publish `SystemNode`
content as well.


