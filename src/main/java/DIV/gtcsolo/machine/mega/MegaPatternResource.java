package DIV.gtcsolo.machine.mega;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.zip.GZIPInputStream;

/** Keeps only the small geometry header; full JSON belongs to one native pattern construction. */
public final class MegaPatternResource {
    private final String resource, name;
    private final Supplier<InputStream> open;
    private volatile Header header;

    public MegaPatternResource(Class<?> owner, String resource, String name) {
        this(resource, name, () -> owner.getResourceAsStream(resource));
    }

    MegaPatternResource(String resource, String name, Supplier<InputStream> open) {
        this.resource = Objects.requireNonNull(resource);
        this.name = Objects.requireNonNull(name);
        this.open = Objects.requireNonNull(open);
    }

    public record Header(int width, int height, int depth, int anchorX, int anchorY, int anchorZ) {
        public Header {
            if (width < 1 || height < 1 || depth < 1 || anchorX < 0 || anchorX >= width
                    || anchorY < 0 || anchorY >= height || anchorZ < 0 || anchorZ >= depth)
                throw new IllegalStateException("Invalid mega pattern dimensions or controller");
        }
    }

    /** Does not read or expand the aisle array. */
    public Header header() {
        Header result = header;
        if (result != null) return result;
        synchronized (this) {
            if (header != null) return header;
            try (var input = input(); var reader = new JsonReader(input)) {
                reader.beginObject();
                JsonObject metadata = new JsonObject();
                while (reader.hasNext()) {
                    String key = reader.nextName();
                    if (key.equals("aisles")) return header = metadata(metadata);
                    if (key.equals("schema") || key.equals("size") || key.equals("controller") || key.equals("axes"))
                        metadata.add(key, JsonParser.parseReader(reader));
                    else reader.skipValue();
                }
                throw new IllegalStateException("Missing mega pattern aisles");
            } catch (Exception failure) { throw failure(failure); }
        }
    }

    /** Called by the already memoized native pattern factory, never by registration or JEI indexing. */
    public JsonObject readLayout() {
        try (var input = input()) {
            JsonObject layout = JsonParser.parseReader(input).getAsJsonObject();
            Header parsed = metadata(layout);
            validateAisles(layout.getAsJsonArray("aisles"), parsed);
            synchronized (this) {
                if (header != null && !header.equals(parsed)) throw new IllegalStateException("Mega pattern header changed");
                header = parsed;
            }
            return layout;
        } catch (Exception failure) { throw failure(failure); }
    }

    private InputStreamReader input() throws Exception {
        InputStream stream = open.get();
        if (stream == null) throw new IllegalStateException("Missing " + name + " pattern: " + resource);
        try {
            return new InputStreamReader(new GZIPInputStream(stream), StandardCharsets.UTF_8);
        } catch (Exception failure) { stream.close(); throw failure; }
    }

    private IllegalStateException failure(Exception cause) {
        return new IllegalStateException("Cannot read " + name + " pattern: " + resource, cause);
    }

    private static Header metadata(JsonObject layout) {
        if (layout.get("schema").getAsInt() != 1) throw new IllegalStateException("Unsupported mega pattern schema");
        JsonArray size = layout.getAsJsonArray("size"), anchor = layout.getAsJsonArray("controller");
        JsonArray axes = layout.getAsJsonArray("axes");
        if (size.size() != 3 || anchor.size() != 3 || axes.size() != 3
                || !axes.get(0).getAsString().equals("RIGHT") || !axes.get(1).getAsString().equals("UP")
                || !axes.get(2).getAsString().equals("BACK")) throw new IllegalStateException("Invalid mega pattern metadata");
        return new Header(size.get(0).getAsInt(), size.get(1).getAsInt(), size.get(2).getAsInt(),
                anchor.get(0).getAsInt(), anchor.get(1).getAsInt(), anchor.get(2).getAsInt());
    }

    private static void validateAisles(JsonArray aisles, Header header) {
        if (aisles.size() != header.depth()) throw new IllegalStateException("Mega pattern depth mismatch");
        int controllers = 0;
        for (int z = 0; z < aisles.size(); z++) {
            JsonArray rows = aisles.get(z).getAsJsonArray();
            if (rows.size() != header.height()) throw new IllegalStateException("Mega pattern height mismatch");
            for (int y = 0; y < rows.size(); y++) {
                String row = rows.get(y).getAsString();
                if (row.length() != header.width()) throw new IllegalStateException("Mega pattern width mismatch");
                for (int x = row.indexOf('K'); x >= 0; x = row.indexOf('K', x + 1)) {
                    controllers++;
                    if (x != header.anchorX() || y != header.anchorY() || z != header.anchorZ())
                        throw new IllegalStateException("Mega pattern controller metadata mismatch");
                }
            }
        }
        if (controllers != 1) throw new IllegalStateException("Expected one mega pattern controller");
    }
}
