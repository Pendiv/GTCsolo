package DIV.gtcsolo.integration.jei.multiblock;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Local authoring overrides. Failed reads/writes never replace the last saved document. */
public final class DocumentationStore {
    public static final int TEXT_LIMIT = 65536;
    private final Path file;
    private Map<String, Text> pages = Map.of();
    private IOException loadError;

    public record Text(String title, String body, String details) {
        public static final Text EMPTY = new Text(null, null, null);
        public Text {
            for (String value : new String[]{title, body, details})
                if (value != null && value.length() > TEXT_LIMIT) throw new IllegalArgumentException("Document is too long");
        }
        public boolean isEmpty() { return title == null && body == null && details == null; }
        public Text apply(Text defaults) {
            return new Text(title == null ? defaults.title : title, body == null ? defaults.body : body,
                    details == null ? defaults.details : details);
        }
        public Text difference(Text defaults) {
            return new Text(Objects.equals(title, defaults.title) ? null : title,
                    Objects.equals(body, defaults.body) ? null : body,
                    Objects.equals(details, defaults.details) ? null : details);
        }
    }

    public DocumentationStore(Path directory, String language) {
        if (!language.matches("[a-z0-9_-]+")) throw new IllegalArgumentException("Invalid language");
        file = directory.resolve(language + ".json");
        reload();
    }
    public Path file() { return file; }
    public Text get(String id) { return pages.getOrDefault(id, Text.EMPTY); }
    public IOException loadError() { return loadError; }

    public void reload() {
        try { pages = read(); loadError = null; }
        catch (IOException error) { loadError = error; }
    }

    private Map<String, Text> read() throws IOException {
        if (!Files.exists(file)) return Map.of();
        if (Files.size(file) > 8 * 1024 * 1024) throw new IOException("Documentation file is too large: " + file);
        try (var reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            var root = JsonParser.parseReader(reader).getAsJsonObject();
            if (root.get("version").getAsInt() != 1) throw new IOException("Unsupported documentation format: " + file);
            var result = new LinkedHashMap<String, Text>();
            for (var entry : root.getAsJsonObject("pages").entrySet()) {
                validateId(entry.getKey());
                var value = entry.getValue().getAsJsonObject();
                result.put(entry.getKey(), new Text(string(value, "title"), string(value, "body"), string(value, "details")));
            }
            return Map.copyOf(result);
        } catch (RuntimeException error) { throw new IOException("Invalid documentation file: " + file, error); }
    }

    private static String string(JsonObject object, String key) {
        if (!object.has(key)) return null;
        if (!object.get(key).isJsonPrimitive() || !object.getAsJsonPrimitive(key).isString())
            throw new IllegalArgumentException("Expected text: " + key);
        return object.get(key).getAsString();
    }
    private static void validateId(String id) {
        if (!id.matches("(?:glossary|multiblock)/[a-z0-9_./:-]+")) throw new IllegalArgumentException("Invalid document id");
    }

    /** Merge other pages from disk, but do not overwrite concurrent edits to this page. */
    public void save(String id, Text expected, Text value) throws IOException {
        validateId(id);
        if (loadError != null) throw new IOException("Cannot overwrite unreadable documentation", loadError);
        var current = new LinkedHashMap<>(read());
        if (!current.getOrDefault(id, Text.EMPTY).equals(expected)) throw new IOException("Document changed on disk; reopen it before saving");
        if (value.isEmpty()) current.remove(id); else current.put(id, value);
        var root = new JsonObject(); root.addProperty("version", 1);
        var entries = new JsonObject();
        current.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
            var object = new JsonObject(); var text = entry.getValue();
            if (text.title != null) object.addProperty("title", text.title);
            if (text.body != null) object.addProperty("body", text.body);
            if (text.details != null) object.addProperty("details", text.details);
            entries.add(entry.getKey(), object);
        });
        root.add("pages", entries);
        Files.createDirectories(file.getParent());
        Path temporary = Files.createTempFile(file.getParent(), "documentation-", ".tmp");
        try {
            Files.writeString(temporary, new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(root) + "\n", StandardCharsets.UTF_8);
            if (Files.exists(file)) Files.copy(file, file.resolveSibling(file.getFileName() + ".bak"), StandardCopyOption.REPLACE_EXISTING);
            try { Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
            catch (AtomicMoveNotSupportedException ignored) { Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING); }
            pages = Map.copyOf(current);
        } finally { Files.deleteIfExists(temporary); }
    }
}
