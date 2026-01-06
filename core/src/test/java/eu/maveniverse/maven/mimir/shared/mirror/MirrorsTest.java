package eu.maveniverse.maven.mimir.shared.mirror;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import eu.maveniverse.maven.mimir.shared.SessionConfig;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.eclipse.aether.repository.RemoteRepository;
import org.junit.jupiter.api.Test;

public class MirrorsTest {
    @Test
    void nullAndEmptyValue() {
        Map<String, List<RemoteRepository>> mirrors =
                Mirrors.parseMirrors(SessionConfig.defaults().build(), null);
        assertTrue(mirrors.isEmpty());

        mirrors = Mirrors.parseMirrors(SessionConfig.defaults().build(), Set.of());
        assertTrue(mirrors.isEmpty());
    }

    @Test
    void smoke() {
        Map<String, List<RemoteRepository>> mirrors = Mirrors.parseMirrors(
                SessionConfig.defaults().build(),
                Set.of(
                        "central(foo::urlfoo, bar::urlbar)",
                        "other(another::contentType::urlanother)",
                        "justurl(just)"));
        assertEquals(3, mirrors.size());

        List<RemoteRepository> repoMirror;

        repoMirror = mirrors.get("central");
        assertNotNull(repoMirror);
        assertEquals(2, repoMirror.size());
        assertEquals("foo", repoMirror.get(0).getId());
        assertEquals("default", repoMirror.get(0).getContentType());
        assertEquals("urlfoo", repoMirror.get(0).getUrl());
        assertEquals("bar", repoMirror.get(1).getId());
        assertEquals("default", repoMirror.get(1).getContentType());
        assertEquals("urlbar", repoMirror.get(1).getUrl());

        repoMirror = mirrors.get("other");
        assertNotNull(repoMirror);
        assertEquals(1, repoMirror.size());
        assertEquals("another", repoMirror.get(0).getId());
        assertEquals("contentType", repoMirror.get(0).getContentType());
        assertEquals("urlanother", repoMirror.get(0).getUrl());

        repoMirror = mirrors.get("justurl");
        assertNotNull(repoMirror);
        assertEquals(1, repoMirror.size());
        assertEquals("justurl", repoMirror.get(0).getId());
        assertEquals("default", repoMirror.get(0).getContentType());
        assertEquals("just", repoMirror.get(0).getUrl());
    }
}
