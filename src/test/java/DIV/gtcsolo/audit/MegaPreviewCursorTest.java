package DIV.gtcsolo.audit;

import DIV.gtcsolo.integration.jei.multiblock.MegaPreviewCursor;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;

/** Pure streaming regression, also runnable with -Xmx64m. No Minecraft/GL initialization. */
public final class MegaPreviewCursorTest {
    public static void main(String[] args) throws Exception { run(); }
    public static void run() throws Exception {
        for (var name : new String[]{"mega_mixer", "ore_processing_factory", "mega_alloy_blast_furnace"}) {
            try (var input = MegaPreviewCursorTest.class.getResourceAsStream("/data/gtcsolo/multiblocks/" + name + ".json.gz")) {
                if (input == null) throw new AssertionError(name);
                try (var cursor = new MegaPreviewCursor(new InputStreamReader(new GZIPInputStream(input), StandardCharsets.UTF_8))) {
                    long[] tally = {0, 0xcbf29ce484222325L};
                    int[] controllers = {0};
                    assert cursor.visited() == 0 : "construction must not scan the volume";
                    assert cursor.step(3, 0, (x,y,z,c) -> { throw new AssertionError("expired deadline"); }) == 0;
                    while (!cursor.done()) {
                        long before = cursor.visited();
                        int visited = cursor.step(997, Long.MAX_VALUE, (x,y,z,c) -> {
                            assert c != ' ' && c != '#';
                            assert x >= 0 && x < cursor.width() && y >= 0 && y < cursor.height() && z >= 0 && z < cursor.depth();
                            tally[0]++;
                            long packed = ((long) (z * cursor.height() + y) * cursor.width() + x) * 65536 + c;
                            tally[1] = (tally[1] ^ packed) * 0x100000001b3L;
                            if (c == 'K') controllers[0]++;
                        });
                        assert visited <= 997 && cursor.visited() - before == visited : "cell budget exceeded";
                    }
                    assert cursor.visited() == cursor.volume();
                    assert controllers[0] == 1;
                    long expected = switch (name) {
                        case "mega_mixer" -> 83_840;
                        case "ore_processing_factory" -> 147_455;
                        default -> 429_782;
                    };
                    assert tally[0] == expected : name + " nonair count " + tally[0];
                    System.out.println("Mega preview cursor " + name + ": " + cursor.visited() + " visited, " + tally[0]
                            + " emitted, checksum " + Long.toUnsignedString(tally[1], 16));
                }
            }
        }
        String header = "{\"size\":[3,1,1],\"palette\":{},\"aisles\":[[\" K#\"]]}";
        try (var cancelled = new MegaPreviewCursor(new StringReader(header))) {
            assert cancelled.step(1, Long.MAX_VALUE, (x,y,z,c) -> { throw new AssertionError(); }) == 1;
            cancelled.close();
            assert cancelled.step(100, Long.MAX_VALUE, (x,y,z,c) -> { throw new AssertionError("cancelled"); }) == 0;
        }
        try (var bad = new MegaPreviewCursor(new StringReader(header.replace(" K#", " K")))) {
            try { bad.step(10, Long.MAX_VALUE, (x,y,z,c) -> {}); throw new AssertionError("invalid width accepted"); }
            catch (IOException expected) { /* malformed resource rejected */ }
        }
    }
}
