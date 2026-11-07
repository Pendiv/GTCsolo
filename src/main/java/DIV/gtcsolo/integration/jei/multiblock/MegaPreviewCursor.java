package DIV.gtcsolo.integration.jei.multiblock;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;

import java.io.Closeable;
import java.io.IOException;
import java.io.Reader;

/** Streaming, bounded traversal. Air/any never become position objects or scene entries. */
public final class MegaPreviewCursor implements Closeable {
    @FunctionalInterface public interface CellConsumer {
        void accept(int x, int y, int z, char symbol);
    }
    private JsonReader reader;
    private final JsonObject palette;
    private final int width, height, depth;
    private int x, y, z;
    private String row;
    private long visited;
    private boolean done;

    public MegaPreviewCursor(Reader input) throws IOException {
        reader = new JsonReader(input);
        JsonObject foundPalette = null;
        int[] size = null;
        try {
            reader.beginObject();
            boolean found = false;
            while (reader.hasNext()) {
                String key = reader.nextName();
                if (key.equals("size")) {
                    var array = JsonParser.parseReader(reader).getAsJsonArray();
                    if (array.size() != 3) throw new IOException("Expected three mega preview dimensions");
                    size = new int[]{array.get(0).getAsInt(), array.get(1).getAsInt(), array.get(2).getAsInt()};
                } else if (key.equals("palette")) {
                    foundPalette = JsonParser.parseReader(reader).getAsJsonObject();
                } else if (key.equals("aisles")) {
                    if (size == null || foundPalette == null) throw new IOException("Missing preview header");
                    reader.beginArray();
                    found = true;
                    break;
                } else reader.skipValue();
            }
            if (!found) throw new IOException("Missing mega preview aisles");
            for (int n : size) if (n < 1 || n > 512) throw new IOException("Invalid mega preview size");
            if ((long) size[0] * size[1] * size[2] > 128_000_000L)
                throw new IOException("Mega preview exceeds supported volume");
            width = size[0]; height = size[1]; depth = size[2]; palette = foundPalette;
            if (!reader.hasNext()) throw new IOException("Empty mega preview");
            reader.beginArray();
        } catch (IOException | RuntimeException failure) {
            close();
            throw failure;
        }
    }

    /** At most maxCells are visited; the caller's frame deadline is also respected. */
    public int step(int maxCells, long deadlineNanos, CellConsumer consumer) throws IOException {
        if (maxCells < 1) throw new IllegalArgumentException("maxCells must be positive");
        if (done || reader == null) return 0;
        int count = 0;
        while (count < maxCells && System.nanoTime() < deadlineNanos) {
            if (row == null) {
                if (!reader.hasNext()) {
                    reader.endArray();
                    if (y != height) throw new IOException("Mega preview row count mismatch");
                    z++; y = 0;
                    if (!reader.hasNext()) {
                        reader.endArray();
                        if (z != depth || visited != volume()) throw new IOException("Mega preview depth mismatch");
                        done = true;
                        close();
                        return count;
                    }
                    if (z >= depth) throw new IOException("Too many mega preview aisles");
                    reader.beginArray();
                    continue;
                }
                if (y >= height) throw new IOException("Too many mega preview rows");
                row = reader.nextString(); x = 0;
                if (row.length() != width) throw new IOException("Mega preview row width mismatch");
            }
            char symbol = row.charAt(x);
            if (symbol != ' ' && symbol != '#') consumer.accept(x, y, z, symbol);
            x++; count++; visited++;
            if (x == width) { row = null; y++; }
        }
        return count;
    }

    public JsonObject palette() { return palette; }
    public int width() { return width; }
    public int height() { return height; }
    public int depth() { return depth; }
    public long visited() { return visited; }
    public long volume() { return (long) width * height * depth; }
    public boolean done() { return done; }
    @Override public void close() throws IOException {
        row = null;
        if (reader != null) {
            var previous = reader; reader = null;
            previous.close();
        }
    }
}
