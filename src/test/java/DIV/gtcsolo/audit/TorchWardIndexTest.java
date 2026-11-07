package DIV.gtcsolo.audit;

import DIV.gtcsolo.block.torch.TorchWardIndex;
import DIV.gtcsolo.block.torch.VoltageTorchTier;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/** Production index versus an independent full-scan oracle; no Minecraft world is launched. */
public final class TorchWardIndexTest {
    private static final int[] RADII = {0, 17, 23, 33, 47, 65, 87, 113, 143, 177, 215, 257, 303, 353, 407};
    private static long checks;
    private record Point(int x, int y, int z) {}

    public static void main(String[] args) {
        boundaries();
        replacementAndSnapshots();
        chunksAndDimensions();
        randomOracle();
        invalidQueries();
        System.out.println("Torch ward index regressions passed: " + checks
                + " checks; all 14 tiers, sphere boundaries, negative/grid coordinates, mutation,"
                + " source chunks, dimension isolation, full-scan oracle, and extreme queries.");
    }

    private static void boundaries() {
        int[] coordinates = {Integer.MIN_VALUE, -30_000_000, -1025, -1024, -65, -64, -1, 0,
                63, 64, 1023, 1024, 30_000_000, Integer.MAX_VALUE};
        int[][] faces = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};
        for (int tier = 1; tier <= 14; tier++) {
            check(VoltageTorchTier.radiusForLevel(tier) == RADII[tier], "tier radius " + tier);
            for (int c : coordinates) {
                TorchWardIndex index = new TorchWardIndex();
                index.put(c, c, c, tier);
                double centre = c + .5;
                check(index.covering(centre, centre, centre).size() == 1, "block centre");
                for (int[] face : faces) {
                    double[] exact = {centre + face[0] * RADII[tier], centre + face[1] * RADII[tier],
                            centre + face[2] * RADII[tier]};
                    check(index.covering(exact[0], exact[1], exact[2]).size() == 1, "inclusive sphere boundary");
                    int axis = face[0] != 0 ? 0 : face[1] != 0 ? 1 : 2;
                    double[] inside = exact.clone(), outside = exact.clone();
                    // A single query ULP can disappear when subtracting a much larger centre.
                    // Use a sub-block epsilon representable on both sides of the subtraction.
                    double epsilon = Math.max(Math.ulp(centre) * 8, 1e-7);
                    inside[axis] -= face[axis] * epsilon;
                    outside[axis] += face[axis] * epsilon;
                    check(index.covering(inside[0], inside[1], inside[2]).size() == 1, "just inside sphere");
                    check(index.covering(outside[0], outside[1], outside[2]).isEmpty(), "just outside sphere");
                }
                double diagonal = RADII[tier] * .75;
                check(index.covering(centre + diagonal, centre + diagonal, centre).isEmpty(), "sphere is not a cube");
                diagonal = RADII[tier] * .5;
                check(index.covering(centre + diagonal, centre + diagonal, centre + diagonal).size() == 1,
                        "inner diagonal");
            }
        }
        TorchWardIndex far = new TorchWardIndex();
        far.put(0, 64, 0, 14);
        check(far.covering(400.5, 64.5, .5).size() == 1, "source 25 chunks away remains covered");
        check(far.covering(.5, 471.5, .5).size() == 1, "vertical MAX radius");
        check(far.covering(.5, 472.5, .5).isEmpty(), "beyond vertical MAX radius");
    }

    private static void replacementAndSnapshots() {
        TorchWardIndex index = new TorchWardIndex();
        check(index.put(-1, -64, 63, 1), "initial insert");
        check(!index.put(-1, -64, 63, 1), "idempotent registration");
        check(index.size() == 1, "duplicate position not multiplied");
        check(index.put(-1, -64, 63, 14), "upgrading moves size band");
        check(index.size() == 1 && index.covering(399.5, -63.5, 63.5).size() == 1, "upgraded range");
        var all = index.all();
        var covering = index.covering(-.5, -63.5, 63.5);
        var inChunk = index.inChunk(-1, 3);
        check(index.put(-1, -64, 63, 1), "downgrading moves size band");
        check(index.covering(399.5, -63.5, 63.5).isEmpty(), "old size-band membership removed");
        check(all.iterator().next().tier() == 14 && covering.get(0).tier() == 14 && inChunk.get(0).tier() == 14,
                "lookup returns detached snapshots while repairing sources");
        for (int bad : new int[]{Integer.MIN_VALUE, -1, 0, 15, Integer.MAX_VALUE}) {
            rejects(() -> index.put(-1, -64, 63, bad));
            check(index.size() == 1 && index.all().iterator().next().tier() == 1, "invalid tier is atomic");
            rejects(() -> VoltageTorchTier.radiusForLevel(bad));
        }
        check(index.remove(-1, -64, 63), "remove existing");
        check(!index.remove(-1, -64, 63), "remove absent");
        check(index.size() == 0 && index.all().isEmpty() && index.inChunk(-1, 3).isEmpty()
                && index.covering(-.5, -63.5, 63.5).isEmpty(), "all index paths cleared");
        check(index.put(-1, -64, 63, 14), "reinsert after removing final band");
    }

    private static void chunksAndDimensions() {
        TorchWardIndex first = new TorchWardIndex(), second = new TorchWardIndex();
        int[] locations = {-33, -32, -17, -16, -1, 0, 15, 16, 31, 32};
        Map<Point, Integer> reference = new HashMap<>();
        for (int x : locations) for (int z : locations) {
            first.put(x, -64, z, 14);
            reference.put(new Point(x, -64, z), 14);
        }
        check(second.covering(.5, -63.5, .5).isEmpty(), "independent dimension has no wards");
        second.put(0, 64, 0, 1);
        check(second.size() == 1 && first.size() == 100, "dimension modifications isolated");
        for (int cx = -4; cx <= 4; cx++) for (int cz = -4; cz <= 4; cz++) {
            Set<Point> expected = new HashSet<>();
            for (Point p : reference.keySet()) {
                if (Math.floorDiv(p.x, 16) == cx && Math.floorDiv(p.z, 16) == cz) expected.add(p);
            }
            check(points(first.inChunk(cx, cz)).equals(expected), "source chunk floor division");
        }
    }

    private static void randomOracle() {
        Random random = new Random(0x746f726368L);
        TorchWardIndex index = new TorchWardIndex();
        Map<Point, Integer> reference = new HashMap<>();
        Point[] pool = new Point[800];
        for (int i = 0; i < pool.length; i++) {
            pool[i] = new Point(random.nextInt(4097) - 2048, random.nextInt(1025) - 512,
                    random.nextInt(4097) - 2048);
        }
        for (int step = 0; step < 6000; step++) {
            Point p = pool[random.nextInt(pool.length)];
            if (random.nextInt(5) == 0) {
                check(index.remove(p.x, p.y, p.z) == (reference.remove(p) != null), "oracle deletion result");
            } else {
                int tier = random.nextInt(14) + 1;
                Integer previous = reference.put(p, tier);
                check(index.put(p.x, p.y, p.z, tier) == (previous == null || previous != tier), "oracle insertion result");
            }
            check(index.size() == reference.size(), "oracle size");
            for (int attempt = 0; attempt < 3; attempt++) {
                double x = p.x + .5 + random.nextDouble() * 1000 - 500;
                double y = p.y + .5 + random.nextDouble() * 1000 - 500;
                double z = p.z + .5 + random.nextDouble() * 1000 - 500;
                Set<Point> expected = new HashSet<>();
                for (var entry : reference.entrySet()) {
                    Point source = entry.getKey();
                    double dx = x - source.x - .5, dy = y - source.y - .5, dz = z - source.z - .5;
                    double radius = RADII[entry.getValue()];
                    if (dx * dx + dy * dy + dz * dz <= radius * radius) expected.add(source);
                }
                var actual = index.covering(x, y, z);
                check(points(actual).equals(expected), "indexed query must match independent full scan at step " + step);
                check(actual.size() == expected.size(), "each ward returned once");
            }
            if (step % 100 == 0) check(points(index.all()).equals(reference.keySet()), "all wards match oracle");
        }
    }

    private static void invalidQueries() {
        TorchWardIndex index = new TorchWardIndex();
        index.put(0, 0, 0, 14);
        for (double value : new double[]{Double.NaN, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY,
                -Double.MAX_VALUE, Double.MAX_VALUE, -1e20, 1e20}) {
            check(index.covering(value, .5, .5).isEmpty(), "invalid/far x query terminates empty");
            check(index.covering(.5, value, .5).isEmpty(), "invalid/far y query terminates empty");
            check(index.covering(.5, .5, value).isEmpty(), "invalid/far z query terminates empty");
        }
    }

    private static Set<Point> points(Iterable<TorchWardIndex.Ward> wards) {
        Set<Point> result = new HashSet<>();
        for (var ward : wards) result.add(new Point(ward.position().x(), ward.position().y(), ward.position().z()));
        return result;
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    private static void rejects(Runnable action) {
        checks++;
        try { action.run(); }
        catch (IllegalArgumentException expected) { return; }
        throw new AssertionError("invalid tier accepted");
    }
}
