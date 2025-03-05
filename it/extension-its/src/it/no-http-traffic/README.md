# No HTTP Traffic

This test redirects Central using custom settings.xml and then build with local empty repository.
The build should succeed as Mimir caches are primed, while if Maven tries to go remote, it will
fail.