package eu.maveniverse.maven.mimir.node.minio;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import org.junit.jupiter.api.Test;

public class UtilsTest {
    @Test
    void roundtrip() {
        Map<String, String> map = Map.of("One", "1", "Two", "2", "THREE", "3");
        System.out.println(map);
        Map<String, String> pushed = Utils.pushMap(map);
        System.out.println(pushed);
        Map<String, String> popped = Utils.popMap(pushed);
        System.out.println(popped);
        assertEquals(map, popped);
    }
}
