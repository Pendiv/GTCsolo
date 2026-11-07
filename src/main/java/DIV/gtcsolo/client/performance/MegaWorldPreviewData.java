package DIV.gtcsolo.client.performance;

import DIV.gtcsolo.integration.jei.multiblock.MegaPreviewCursor;
import DIV.gtcsolo.machine.mega.MegaPatternResource;
import com.google.gson.JsonObject;
import it.unimi.dsi.fastutil.longs.Long2CharOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongIterator;

import java.io.IOException;
import java.io.Reader;

/** Streaming geometry only: no BlockPos, BlockInfo, dense array, dummy world or GPU object. */
public final class MegaWorldPreviewData implements AutoCloseable {
    private final MegaPreviewCursor cursor;
    private final MegaPatternResource.Header header;
    private final char port;
    private final Long2CharOpenHashMap symbols = new Long2CharOpenHashMap();
    private final LongArrayList ports = new LongArrayList();
    private int controllers;
    private boolean closed, validated;

    public MegaWorldPreviewData(Reader input, MegaPatternResource.Header header, char port) throws IOException {
        this.header = header;
        this.port = port;
        cursor = new MegaPreviewCursor(input);
        if (cursor.width() != header.width() || cursor.height() != header.height() || cursor.depth() != header.depth()) {
            cursor.close();
            throw new IOException("Mega world-preview header mismatch");
        }
    }

    public int step(int maxCells, long deadline) throws IOException {
        if (closed || validated) return 0;
        int visited = cursor.step(maxCells, deadline, (x, y, z, symbol) -> {
            long position = pack(x, y, z);
            symbols.put(position, symbol);
            if (symbol == port) ports.add(position);
            if (symbol == 'K') {
                controllers++;
                if (x != header.anchorX() || y != header.anchorY() || z != header.anchorZ())
                    throw new IllegalStateException("Mega world-preview controller mismatch");
            }
        });
        if (cursor.done()) {
            if (controllers != 1) throw new IOException("Expected one mega world-preview controller");
            validated = true;
        }
        return visited;
    }

    public boolean done() { return validated; }
    public long visited() { return cursor.visited(); }
    public long volume() { return cursor.volume(); }
    public int nonAirCount() { return symbols.size(); }
    public JsonObject palette() { return cursor.palette(); }
    public MegaPatternResource.Header header() { return header; }
    public char symbol(long position) { return symbols.get(position); }
    public LongIterator positions() { return symbols.keySet().iterator(); }
    public int portCount() { return ports.size(); }
    public long portPosition(int index) { return ports.getLong(index); }

    // The streaming cursor restricts every axis to 512, so unsigned 16-bit local coordinates suffice.
    public static long pack(int x, int y, int z) { return (long) x << 32 | (long) y << 16 | z; }
    public static int x(long position) { return (int) (position >>> 32); }
    public static int y(long position) { return (int) (position >>> 16 & 0xffff); }
    public static int z(long position) { return (int) (position & 0xffff); }

    @Override public void close() throws IOException {
        closed = true;
        try { cursor.close(); }
        finally { symbols.clear(); ports.clear(); }
    }
}
