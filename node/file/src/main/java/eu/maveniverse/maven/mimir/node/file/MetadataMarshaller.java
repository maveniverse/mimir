package eu.maveniverse.maven.mimir.node.file;

import eu.maveniverse.maven.shared.core.fs.FileUtils;
import eu.maveniverse.maven.shared.core.maven.MavenUtils;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Metadata marshaller is doing the metadata persistence to and from disk.
 *
 * @since 0.11.0
 */
public interface MetadataMarshaller {
    /**
     * Loads in metadata from file.
     */
    Map<String, String> load(Path md) throws IOException;

    /**
     * Writes out metadata to file.
     */
    void save(Path md, Map<String, String> metadata) throws IOException;

    /**
     * Uses standard Java Properties files (with tweaks) to marshal metadata. Produces stable output.
     */
    class PropertiesMetadataMarshaller implements MetadataMarshaller {
        @Override
        public Map<String, String> load(Path md) throws IOException {
            try (InputStream is = Files.newInputStream(md)) {
                Properties properties = new Properties();
                properties.load(is);
                return MavenUtils.toMap(properties);
            }
        }

        @Override
        public void save(Path md, Map<String, String> metadata) throws IOException {
            Properties properties = new Properties();
            properties.putAll(metadata);
            FileUtils.writeFile(md, path -> writeProperties(properties, path));
        }

        /**
         * Writes out Java Properties file with tweaks:
         * <ul>
         *     <li>does not write out comment</li>
         *     <li>does not write out timestamp (standard Java properties format)</li>
         *     <li>uses stable ordering for entries</li>
         * </ul>
         * Reason: to keep whole file structure unchanged, if no real change happened (even if file is rewritten).
         */
        private void writeProperties(Properties properties, Path path) throws IOException {
            StringWriter sw = new StringWriter();
            properties.store(sw, null);
            ArrayList<String> lines =
                    new ArrayList<>(Arrays.asList(sw.toString().split("\\R")));
            lines.remove(0);
            lines.sort(String::compareTo); // make lines ordering stable
            try (OutputStream os = Files.newOutputStream(
                            path, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                    BufferedWriter w = new BufferedWriter(new OutputStreamWriter(os, StandardCharsets.ISO_8859_1))) {
                for (String line : lines) {
                    w.write(line);
                    w.newLine();
                }
                w.flush();
            }
        }
    }

    /**
     * Uses {@link ObjectOutputStream} (with tweaks) to marshal metadata. Produces stable output.
     */
    class ObjectOutputStreamMetadataMarshaller implements MetadataMarshaller {
        @Override
        public Map<String, String> load(Path md) throws IOException {
            try (ObjectInputStream ois = new ObjectInputStream(Files.newInputStream(md))) {
                HashMap<String, String> metadata = new HashMap<>();
                int entries = ois.readInt();
                for (int i = 0; i < entries; i++) {
                    String key = ois.readUTF();
                    String value = ois.readUTF();
                    metadata.put(key, value);
                }
                return metadata;
            }
        }

        @Override
        public void save(Path md, Map<String, String> metadata) throws IOException {
            try (ObjectOutputStream oos = new ObjectOutputStream(
                    Files.newOutputStream(md, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING))) {
                oos.writeInt(metadata.size());
                for (Map.Entry<String, String> entry :
                        metadata.entrySet().stream().sorted().toList()) {
                    oos.writeUTF(entry.getKey());
                    oos.writeUTF(entry.getValue());
                }
            }
        }
    }
}
