package DIV.gtcsolo.audit;

import DIV.gtcsolo.machine.mega.MegaPatternResource;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/** Real resources plus bounded input fixtures; no Minecraft or Forge initialization is required. */
public final class MegaPatternResourceRegressionTest {
    private static final String PREFIX = "{\"schema\":1,\"size\":[2,1,1],\"controller\":[0,0,0],"
            + "\"axes\":[\"RIGHT\",\"UP\",\"BACK\"],\"palette\":{},\"aisles\":";

    public static void main(String[] args) throws Throwable {
        if (!MegaPatternResourceRegressionTest.class.desiredAssertionStatus()) throw new IllegalStateException("Use -ea");
        var opens = new AtomicInteger();
        byte[] malformedBody = gzip(PREFIX + "THIS AISLE BODY IS INTENTIONALLY NOT JSON");
        var streaming = resource(() -> { opens.incrementAndGet(); return new ByteArrayInputStream(malformedBody); });
        assert opens.get() == 0 : "registration/constructor must not open the resource";
        var header = streaming.header();
        assert header.equals(new MegaPatternResource.Header(2, 1, 1, 0, 0, 0));
        assert opens.get() == 1;
        assert streaming.header() == header && opens.get() == 1;
        expectFailure(streaming::readLayout); // A successful header read did not parse the malformed aisle body.

        byte[] valid = gzip(PREFIX + "[[\"K \"]]}");
        opens.set(0);
        var concurrent = resource(() -> { opens.incrementAndGet(); return new ByteArrayInputStream(valid); });
        var start = new CountDownLatch(1);
        var failure = new AtomicReference<Throwable>();
        List<Thread> workers = new ArrayList<>();
        for (int i = 0; i < 16; i++) {
            var worker = new Thread(() -> {
                try { start.await(); assert concurrent.header().equals(header); }
                catch (Throwable caught) { failure.compareAndSet(null, caught); }
            });
            workers.add(worker); worker.start();
        }
        start.countDown();
        for (Thread worker : workers) worker.join();
        if (failure.get() != null) throw failure.get();
        assert opens.get() == 1 : "concurrent header access must share only one bounded read";
        JsonObject first = concurrent.readLayout();
        first.addProperty("testMutation", true);
        JsonObject second = concurrent.readLayout();
        assert !second.has("testMutation") && opens.get() == 3 : "full JSON must not be globally retained or reused";

        for (String body : List.of("[]}", "[[\"KK\"]]}", "[[\" K\"]]}", "[[\"K\"]]}", "[[\"K \",\"  \"]]}"))
            expectFailure(resource(bytes(gzip(PREFIX + body)))::readLayout);
        expectFailure(resource(bytes(gzip(PREFIX.replace("RIGHT", "LEFT") + "[[\"K \"]]}")))::header);
        expectFailure(resource(bytes(gzip(PREFIX.replace("[0,0,0]", "[2,0,0]") + "[[\"K \"]]}")))::header);
        expectFailure(resource(() -> null)::header);

        int layouts = 0;
        for (String name : List.of("mega_mixer", "mega_alloy_blast_furnace", "ore_processing_factory")) {
            String path = "/data/gtcsolo/multiblocks/" + name + ".json.gz";
            var actual = new MegaPatternResource(MegaPatternResourceRegressionTest.class, path, name);
            var actualHeader = actual.header();
            var actualLayout = actual.readLayout();
            try (var input = new InputStreamReader(new GZIPInputStream(
                    MegaPatternResourceRegressionTest.class.getResourceAsStream(path)), StandardCharsets.UTF_8)) {
                assert JsonParser.parseReader(input).equals(actualLayout) : "resource content must remain exact";
            }
            String controllerRow = actualLayout.getAsJsonArray("aisles").get(actualHeader.anchorZ())
                    .getAsJsonArray().get(actualHeader.anchorY()).getAsString();
            assert controllerRow.charAt(actualHeader.anchorX()) == 'K';
            layouts++;
        }
        System.out.println("PASS: no-read construction, header without aisle parsing, 16 concurrent readers, no retained JSON,"
                + " invalid geometry/header detection, and " + layouts + " exact real mega layouts.");
    }

    private static MegaPatternResource resource(Supplier<InputStream> open) throws Exception {
        var constructor = MegaPatternResource.class.getDeclaredConstructor(String.class, String.class, Supplier.class);
        constructor.setAccessible(true);
        return constructor.newInstance("fixture.json.gz", "fixture", open);
    }

    private static Supplier<InputStream> bytes(byte[] value) { return () -> new ByteArrayInputStream(value); }
    private static byte[] gzip(String text) throws Exception {
        var bytes = new ByteArrayOutputStream();
        try (var gzip = new GZIPOutputStream(bytes)) { gzip.write(text.getBytes(StandardCharsets.UTF_8)); }
        return bytes.toByteArray();
    }
    private static void expectFailure(Supplier<?> call) {
        try { call.get(); throw new AssertionError("Expected malformed/missing resource rejection"); }
        catch (IllegalStateException expected) { }
    }
}
