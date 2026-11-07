package DIV.gtcsolo.audit;

import DIV.gtcsolo.client.performance.MegaWorldPreviewData;
import DIV.gtcsolo.machine.mega.MegaPatternResource;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;

/** Run with -Xmx64m: the shipped largest export must never require a dense scene or JSON tree. */
public final class MegaWorldPreviewDataRegressionTest {
    private static int assertions;

    public static void main(String[] arguments) throws Exception {
        check("mega_mixer", 'T', 120, 101, 89, 62, 8, 2, 83_840, 43);
        check("mega_alloy_blast_furnace", '%', 189, 225, 182, 59, 6, 11, 429_782, 48);
        check("ore_processing_factory", '7', 195, 93, 157, 103, 6, 3, 147_455, 67);
        malformedAndClosed();
        System.out.println("PASS MegaWorldPreviewDataRegressionTest: " + assertions + " assertions, max heap "
                + Runtime.getRuntime().maxMemory());
    }

    private static void check(String name, char port, int width, int height, int depth,
                              int anchorX, int anchorY, int anchorZ, int count, int portCount) throws Exception {
        String path = "/data/gtcsolo/multiblocks/" + name + ".json.gz";
        var header = new MegaPatternResource.Header(width, height, depth, anchorX, anchorY, anchorZ);
        try (var stream = MegaWorldPreviewDataRegressionTest.class.getResourceAsStream(path);
             var data = new MegaWorldPreviewData(new InputStreamReader(new GZIPInputStream(stream), StandardCharsets.UTF_8), header, port)) {
            require(data.visited() == 0 && data.nonAirCount() == 0 && !data.done(), "constructor must not expand aisles");
            require(data.step(997, System.nanoTime() - 1) == 0 && data.visited() == 0, "expired frame budget");
            while (!data.done()) {
                long before = data.visited();
                int advanced = data.step(997, Long.MAX_VALUE);
                require(advanced <= 997 && data.visited() - before == advanced, "bounded traversal");
            }
            require(data.visited() == (long) width * height * depth, "full geometric volume");
            require(data.nonAirCount() == count && data.portCount() == portCount, "independent exported block/port counts");
            int seen = 0, controllers = 0;
            for (var iterator = data.positions(); iterator.hasNext();) {
                long position = iterator.nextLong();
                int x = MegaWorldPreviewData.x(position), y = MegaWorldPreviewData.y(position), z = MegaWorldPreviewData.z(position);
                require(x >= 0 && x < width && y >= 0 && y < height && z >= 0 && z < depth, "sparse coordinate bounds");
                require(MegaWorldPreviewData.pack(x, y, z) == position, "lossless coordinate storage");
                char symbol = data.symbol(position);
                require(symbol != 0 && symbol != '#' && symbol != ' ', "air never enters the sparse scene");
                if (symbol == 'K') {
                    controllers++;
                    require(x == anchorX && y == anchorY && z == anchorZ, "native controller anchor");
                }
                seen++;
            }
            require(seen == count && controllers == 1, "sparse map contains every exported solid once");
            long previous = -1;
            for (int index = 0; index < portCount; index++) {
                long position = data.portPosition(index);
                long traversalIndex = ((long) MegaWorldPreviewData.z(position) * height + MegaWorldPreviewData.y(position)) * width
                        + MegaWorldPreviewData.x(position);
                require(data.symbol(position) == port && traversalIndex > previous, "native z/y/x port candidate order");
                previous = traversalIndex;
            }
            require(data.step(997, Long.MAX_VALUE) == 0, "completed reader is inert");
        }
    }

    private static void malformedAndClosed() throws Exception {
        var header = new MegaPatternResource.Header(2, 1, 1, 0, 0, 0);
        for (String aisle : new String[]{"[ [\"K\"] ]", "[ [\"  \"] ]", "[ [\" K\"] ]", "[ [\"KK\"] ]"}) {
            boolean failed = false;
            try (var data = new MegaWorldPreviewData(new StringReader("{\"size\":[2,1,1],\"palette\":{},\"aisles\":" + aisle + "}"), header, 'P')) {
                while (!data.done()) data.step(1, Long.MAX_VALUE);
            } catch (IOException | IllegalStateException expected) { failed = true; }
            require(failed, "malformed width/missing/moved/duplicate controller rejected");
        }
        final boolean[] closed = {false};
        StringReader input = new StringReader("{\"size\":[2,1,1],\"palette\":{},\"aisles\":[[\"KP\"]]}") {
            @Override public void close() { closed[0] = true; super.close(); }
        };
        var data = new MegaWorldPreviewData(input, header, 'P');
        data.step(1, Long.MAX_VALUE);
        data.close();
        require(closed[0] && data.nonAirCount() == 0 && data.portCount() == 0, "cancellation closes source and releases sparse storage");
        require(data.step(1, Long.MAX_VALUE) == 0, "closed reader cannot resume");
    }

    private static void require(boolean condition, String message) {
        assertions++;
        if (!condition) throw new AssertionError(message);
    }
}
