package eu.maveniverse.maven.mimir.shared.impl.checksum;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.UncheckedIOException;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class ChecksumEnforcerTest {
    @Test
    void sameIsOk() {
        Map<String, String> expected = Map.of("algA", "aaaaa", "algB", "bbbbb");
        new ChecksumEnforcer(expected).accept(expected);
    }

    @Test
    void supersetIsOk() {
        Map<String, String> expected = Map.of("algA", "aaaaa", "algB", "bbbbb");
        Map<String, String> calculated = Map.of("algA", "aaaaa", "algB", "bbbbb", "algC", "ccccc");
        new ChecksumEnforcer(expected).accept(calculated);
    }

    @Test
    void subsetIsOk() {
        Map<String, String> expected = Map.of("algA", "aaaaa", "algB", "bbbbb");
        Map<String, String> calculated = Map.of("algB", "bbbbb");
        new ChecksumEnforcer(expected).accept(calculated);
    }

    @Test
    void noIntersectionIsNotOk() {
        Map<String, String> expected = Map.of("algA", "aaaaa", "algB", "bbbbb");
        Map<String, String> calculated = Map.of("algD", "ddddd");
        assertThrows(UncheckedIOException.class, () -> new ChecksumEnforcer(expected).accept(calculated));
    }

    @Test
    void mismatchIsNotOk() {
        Map<String, String> expected = Map.of("algA", "aaaaa", "algB", "bbbbb");
        Map<String, String> calculated = Map.of("algA", "aaaaa", "algB", "ooooo");
        assertThrows(UncheckedIOException.class, () -> new ChecksumEnforcer(expected).accept(calculated));
    }
}
