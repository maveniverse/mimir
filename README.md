# Mímir

[![Maven Central](https://img.shields.io/maven-central/v/eu.maveniverse.maven.mimir/extension3.svg?label=Maven%20Central)](https://search.maven.org/artifact/eu.maveniverse.maven.mimir/extension3)

Requirements:
* Java 17+
* Maven 3.9+ (tested with 3.9.11 and 4.0.0-rc-5)

Used by Maven CI among others.

Goal: A system-wide cache for Maven. Will make you to love to nuke your local repository. It adds a layer of
cache between resolver and transport. This implies it works irrelevant of location of your local repository 
and its kind (enhanced, split, whatever). You have one single "local" system-wide cache (by def in `~/.mimir/local`) and it
is consulted before Maven would go remote. Out of the box Mimir caches release artifacts from Maven Central, but it is
not limited to it: you can tell Mimir which remote repositories to cache (see [Which repositories are cached](#which-repositories-are-cached) below).
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
* RemoteRepository handling is configurable: you pick which repositories (by id, with optional filters) get cached; there are still TBDs here, especially around real MRM integration.

## To use it

With Maven 3 create project-wide, or with Maven 4-rc-5+ create user-wide `~/.m2/extensions.xml` like this:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<extensions>
    <extension>
        <groupId>eu.maveniverse.maven.mimir</groupId>
        <artifactId>extension3</artifactId>
        <version>${version.mimir}</version>
    </extension>
</extensions>
```
IF you have docker, Tailscale (or just non-trivial networking setup), help Mimir components (like JGroups and publishers are) 
to select your LAN interface: create `~/.mimir/daemon.properties` file with following content:
```properties
mimir.localHostHint=match-address\:192.168.1.*
```
(use yor LAN IP address).

To make Mimir stop you nagging to auto-update, set `mimir.checkupdates` in `~/.mimir/session.properties`.

More on [site documentation](https://maveniverse.eu/docs/mimir/how-to-use-it/).

And just build with Maven...

Build requirements:
* Java 21
* Maven 3.9.9+

## Which repositories are cached

By default Mimir caches release artifacts coming from Maven Central only. But you are not stuck with
that: the `mimir.session.repositories` setting lets you list exactly which remote repositories Mimir
should cache. Put it in `~/.mimir/session.properties` (or pass it as `-Dmimir.session.repositories=...`):

```properties
mimir.session.repositories=central,foo,bar
```

The value is a comma-separated list of repository specs, and each spec is matched against the remote
repositories your build actually uses. The matching predicates are OR-ed together, so an artifact is
cached if it comes from *any* listed repository.

A spec can be:

* a repository **id** (e.g. `foo`) — matches the remote repository whose id is `foo`;
* the wildcard **`*`** — matches *any* release repository Mimir encounters;
* an id or `*` followed by **modifiers** in parentheses, comma-separated and without spaces, for
  example `foo(releaseOnly,httpsOnly)` or `*(httpsOnly)`.

Available modifiers:

| Modifier      | Meaning                                                                    |
|---------------|----------------------------------------------------------------------------|
| `directOnly`  | Repository is used directly (not a `mirrorOf` target and not an MRM).      |
| `releaseOnly` | Repository has only its release policy enabled (no snapshots).             |
| `httpsOnly`   | Repository is served over HTTPS.                                           |

A couple of things worth knowing:

* Mimir only ever caches **release** artifacts — snapshots are always ignored, because caching mutable
  content would be unsafe. So do not use Mimir if your workflow relies on mutable release artifacts.
* When you leave the setting unset, the effective default is
  `central(directOnly,releaseOnly,httpsOnly)` — i.e. Maven Central, accessed directly over HTTPS.
* If you list `central` as a **plain id** (as in the `central,foo,bar` example above), it no longer
  carries those strict modifiers. If you want to keep Central strict while adding more repositories,
  spell it out: `central(directOnly,releaseOnly,httpsOnly),foo,bar`.


## Resolving Log

Mimir can log every artifact resolution, giving you a full record of what was resolved during a
build — including groupId, artifactId, version, classifier, extension, the repository it came from, the
canonical artifact URL, and whether the file was served from cache or downloaded fresh.

### Enabling

Add `-Dmimir.resolvingLog.globalPath=/some/path` or `-Dmimir.resolvingLog.projectPath=/some/path` to your Maven invocation:

```
mvn verify -Dmimir.resolvingLog.projectPath=/some/path
```

Or set it permanently in `~/.mimir/session.properties`:

```properties
mimir.resolvingLog.globalPath=/some/path
```

### Output files

When enabled, the configured files are written out to configured paths. Global file **accumulates
across builds** — each build appends to it. It persists until you delete it manually (log rotation is
your responsibility if the file grows large).

The `mimir.resolvingLog.globalPath` provided path is resolved against Mimir basedir (default `~/.mimir`).

To also write a per-project log that gets wiped by `mvn clean`, configure a project-relative path:

```properties
mimir.resolvingLog.projectPath=target/mimir-resolving-log.csv
```

The `mimir.resolvingLog.projectPath` is resolved against project basedir.

Both files are written simultaneously when both are configured.

To override the global file location:

```properties
mimir.resolvingLog.globalPath=/path/to/my-resolving-log.csv
```

### Format

The default format is CSV with a header row:

```csv
seq,groupId,artifactId,version,classifier,extension,repositoryId,repositoryUrl,artifactUrl,status,context,scope
2026-05-16T14:46:29.899608696Z@1,org.slf4j,slf4j-api,2.0.17,,pom,central,https://repo.maven.apache.org/maven2,https://repo.maven.apache.org/maven2/org/slf4j/slf4j-api/2.0.17/slf4j-api-2.0.17.pom,cache,project,(model)
2026-05-16T14:46:29.899608696Z@2,org.slf4j,slf4j-parent,2.0.17,,pom,central,https://repo.maven.apache.org/maven2,https://repo.maven.apache.org/maven2/org/slf4j/slf4j-parent/2.0.17/slf4j-parent-2.0.17.pom,cache,project,(model)
2026-05-16T14:46:29.899608696Z@3,org.slf4j,slf4j-bom,2.0.17,,pom,central,https://repo.maven.apache.org/maven2,https://repo.maven.apache.org/maven2/org/slf4j/slf4j-bom/2.0.17/slf4j-bom-2.0.17.pom,cache,project,(model)
2026-05-16T14:46:29.899608696Z@4,org.slf4j,slf4j-api,2.0.17,,jar,central,https://repo.maven.apache.org/maven2,https://repo.maven.apache.org/maven2/org/slf4j/slf4j-api/2.0.17/slf4j-api-2.0.17.jar,cache,project/compile,compile
```

To use JSON Lines instead:

```properties
mimir.resolverLog.format=jsonl
```

```json
{"seq":"2026-05-16T14:46:29.899608696Z@1","groupId":"com.google.guava","artifactId":"guava","version":"33.6.0-jre","classifier":"","extension":"jar","repositoryId":"central","repositoryUrl":"https://repo.maven.apache.org/maven2","artifactUrl":"https://repo.maven.apache.org/maven2/com/google/guava/guava/33.6.0-jre/guava-33.6.0-jre.jar","status":"cache"}
```

The `seq` field is a unique identifier for each resolution event, combining a timestamp with a sequence number to 
ensure uniqueness even for multiple resolutions occurring at the same time. The timestamp is created at the very
first artifact resolution of a Maven session and counter is incremented for each resolution.

### Status values

| Value | Meaning |
|---|---|
| `cache` | Served from the Mimir local cache — no network request was made |
| `remote` | Downloaded from the remote repository and stored in cache |
| `failed` | Download was attempted but failed (network error, 404, etc.) |

All artifact types are logged: `.jar`, `.pom`, `.asc` signatures, `.war`, `.aar`, etc. Internal
existence-check probes are not logged.

### Configuration reference

| Property                     | Default                            | Description                                           |
|------------------------------|------------------------------------|-------------------------------------------------------|
| `mimir.resolvingLog.enabled`     | `false`                            | Enable resolving logging                              |
| `mimir.resolvingLog.path`        | `~/.mimir/mimir-resolving-log.csv` | Path to the global log file                           |
| `mimir.resolvingLog.projectPath` | *(unset)*                          | Optional second log file (relative to execution root) |
| `mimir.resolvingLog.format`      | `csv`                              | Output format: `csv` or `jsonl`                       |

