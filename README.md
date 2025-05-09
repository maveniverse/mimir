# Mímir

[![Maven Central](https://img.shields.io/maven-central/v/eu.maveniverse.maven.mimir/extension.svg?label=Maven%20Central)](https://search.maven.org/artifact/eu.maveniverse.maven.mimir/extension)

Requirements:
* Java 17+
* Maven 3.9+ (tested with 3.9.9 and 4.0.0-rc-3)

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

With Maven 3 create project-wide, or with Maven 4-rc-3+ create user-wide `~/.m2/extensions.xml` like this:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<extensions>
    <extension>
        <groupId>eu.maveniverse.maven.mimir</groupId>
        <artifactId>extension</artifactId>
        <version>0.4.1</version>
    </extension>
</extensions>
```
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

## High level design

TBD
