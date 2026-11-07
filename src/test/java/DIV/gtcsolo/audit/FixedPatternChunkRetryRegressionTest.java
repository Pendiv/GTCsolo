package DIV.gtcsolo.audit;

import DIV.gtcsolo.performance.FixedPatternChunkRetry;
import com.gregtechceu.gtceu.api.pattern.util.RelativeDirection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

/** Checks the necessary loaded-position proof, not a replacement for native matching/formation. */
public final class FixedPatternChunkRetryRegressionTest {
    private static final ResourceLocation OVERWORLD = new ResourceLocation("minecraft", "overworld");
    private static final ResourceLocation NETHER = new ResourceLocation("minecraft", "the_nether");

    public static void main(String[] args) {
        if (!FixedPatternChunkRetryRegressionTest.class.desiredAssertionStatus()) throw new IllegalStateException("Use -ea");
        try {
            checkObservedMegaMixerBoundary();
            checkInvalidationAndDisabledBehavior();
            checkNecessaryConditionAcrossChunkBorders();
            System.out.println("Fixed-pattern chunk retry passed: observed mega-mixer boundary, all horizontal"
                    + " orientations, 500 randomized loaded-chunk fixtures, mutable-position isolation,"
                    + " load recovery/rotation/dimension/movement fallback and disabled no-op.");
        } finally {
            FixedPatternChunkRetry.setEnabled(false);
        }
    }

    private static void checkObservedMegaMixerBoundary() {
        var bounds = new FixedPatternChunkRetry.Bounds(-62, 57, -8, 92, -2, 86);
        var center = new BlockPos(-126, -49, -80);
        var required = new BlockPos(-177, -57, -142);
        assert bounds.contains(center, Direction.EAST, required);
        assert new ChunkPos(required).x == -12 && new ChunkPos(required).z == -9;
        var probe = new LoadedChunks();
        var gate = new FixedPatternChunkRetry(bounds);
        FixedPatternChunkRetry.setEnabled(true);
        gate.rememberUnloaded(OVERWORLD, center, Direction.EAST, Direction.NORTH, false, required, probe::isLoaded);
        Set<Long> before = Set.copyOf(probe.loaded);
        for (int i = 0; i < 100; i++) {
            assert gate.shouldSkip(OVERWORLD, center, Direction.EAST, Direction.NORTH, false, probe::isLoaded);
        }
        assert probe.loaded.equals(before) : "the guard must never create chunks or tickets";
        probe.load(required);
        assert !gate.shouldSkip(OVERWORLD, center, Direction.EAST, Direction.NORTH, false, probe::isLoaded)
                : "native matching must resume on the next ordinary retry after the chunk loads";
        probe.unload(required);
        assert !gate.shouldSkip(OVERWORLD, center, Direction.EAST, Direction.NORTH, false, probe::isLoaded)
                : "observed recovery must discard the old proof";
    }

    private static void checkInvalidationAndDisabledBehavior() {
        var bounds = new FixedPatternChunkRetry.Bounds(-1, 2, -1, 1, -2, 3);
        var center = new BlockPos(15, 40, 15);
        var required = worldPosition(center, Direction.EAST, -1, -1, -2);
        var probe = new LoadedChunks();
        var gate = new FixedPatternChunkRetry(bounds);
        FixedPatternChunkRetry.setEnabled(false);
        gate.rememberUnloaded(OVERWORLD, center, Direction.EAST, Direction.NORTH, false, required, probe::isLoaded);
        assert !gate.shouldSkip(OVERWORLD, center, Direction.EAST, Direction.NORTH, false, probe::isLoaded);
        assert probe.reads == 0 : "disabled mode must not inspect chunks";
        FixedPatternChunkRetry.setEnabled(true);

        remember(gate, probe, center, required);
        assert !gate.shouldSkip(OVERWORLD, center, Direction.NORTH, Direction.NORTH, false, probe::isLoaded);
        assert !gate.shouldSkip(OVERWORLD, center, Direction.EAST, Direction.NORTH, false, probe::isLoaded);
        remember(gate, probe, center, required);
        assert !gate.shouldSkip(NETHER, center, Direction.EAST, Direction.NORTH, false, probe::isLoaded);
        remember(gate, probe, center, required);
        assert !gate.shouldSkip(OVERWORLD, center.above(), Direction.EAST, Direction.NORTH, false, probe::isLoaded);
        remember(gate, probe, center, required);
        assert !gate.shouldSkip(OVERWORLD, center, Direction.EAST, Direction.WEST, false, probe::isLoaded);
        remember(gate, probe, center, required);
        assert !gate.shouldSkip(OVERWORLD, center, Direction.EAST, Direction.NORTH, true, probe::isLoaded);

        BlockPos outside = worldPosition(center, Direction.EAST, 3, 0, 0);
        gate.rememberUnloaded(OVERWORLD, center, Direction.EAST, Direction.NORTH, false, outside, probe::isLoaded);
        assert !gate.shouldSkip(OVERWORLD, center, Direction.EAST, Direction.NORTH, false, probe::isLoaded)
                : "a position outside the fixed footprint proves nothing";
        gate.rememberUnloaded(OVERWORLD, center, Direction.UP, Direction.NORTH, false, required, probe::isLoaded);
        assert !gate.shouldSkip(OVERWORLD, center, Direction.UP, Direction.NORTH, false, probe::isLoaded);

        BlockPos.MutableBlockPos mutableCenter = center.mutable();
        BlockPos.MutableBlockPos mutableRequired = required.mutable();
        gate.rememberUnloaded(OVERWORLD, mutableCenter, Direction.EAST, Direction.NORTH, false,
                mutableRequired, probe::isLoaded);
        mutableCenter.set(1000, 1000, 1000);
        mutableRequired.set(2000, 2000, 2000);
        assert gate.shouldSkip(OVERWORLD, center, Direction.EAST, Direction.NORTH, false, probe::isLoaded);
        assert probe.last.equals(required) : "mutable caller positions must not move a cached proof";
        gate.clear();
        assert !gate.shouldSkip(OVERWORLD, center, Direction.EAST, Direction.NORTH, false, probe::isLoaded);
    }

    private static void checkNecessaryConditionAcrossChunkBorders() {
        var random = new Random(0x47_1_6_4L);
        var bounds = new FixedPatternChunkRetry.Bounds(-3, 3, -1, 1, -3, 3);
        for (int trial = 0; trial < 500; trial++) {
            Direction front = Direction.from2DDataValue(trial % 4);
            BlockPos center = new BlockPos(random.nextInt(65) - 32, random.nextInt(32), random.nextInt(65) - 32);
            Set<Long> footprint = new HashSet<>();
            for (int x = bounds.minX(); x <= bounds.maxX(); x++) {
                for (int z = bounds.minZ(); z <= bounds.maxZ(); z++) {
                    BlockPos position = worldPosition(center, front, x, 0, z);
                    assert bounds.contains(center, front, position);
                    footprint.add(new ChunkPos(position).toLong());
                }
            }
            var probe = new LoadedChunks();
            for (long chunk : footprint) if (random.nextBoolean()) probe.loaded.add(chunk);
            BlockPos nativeFailure = firstUnloaded(bounds, center, front, probe.loaded);
            var gate = new FixedPatternChunkRetry(bounds);
            if (nativeFailure == null) {
                assert !gate.shouldSkip(OVERWORLD, center, front, Direction.NORTH, false, probe::isLoaded);
                continue;
            }
            gate.rememberUnloaded(OVERWORLD, center, front, Direction.NORTH, false, nativeFailure, probe::isLoaded);
            assert gate.shouldSkip(OVERWORLD, center, front, Direction.NORTH, false, probe::isLoaded);
            assert firstUnloaded(bounds, center, front, probe.loaded) != null;
            for (long chunk : footprint) {
                if (chunk != new ChunkPos(nativeFailure).toLong()) probe.loaded.add(chunk);
            }
            assert gate.shouldSkip(OVERWORLD, center, front, Direction.NORTH, false, probe::isLoaded)
                    : "other changes cannot make a still-unloaded mandatory position valid";
            assert firstUnloaded(bounds, center, front, probe.loaded) != null;
            probe.load(nativeFailure);
            assert firstUnloaded(bounds, center, front, probe.loaded) == null;
            assert !gate.shouldSkip(OVERWORLD, center, front, Direction.NORTH, false, probe::isLoaded);
        }
    }

    private static void remember(FixedPatternChunkRetry gate, LoadedChunks probe, BlockPos center, BlockPos required) {
        gate.rememberUnloaded(OVERWORLD, center, Direction.EAST, Direction.NORTH, false, required, probe::isLoaded);
    }

    /** Uses GT's native RIGHT/UP/BACK direction mapping as the independent geometry reference. */
    private static BlockPos worldPosition(BlockPos center, Direction front, int x, int y, int z) {
        return center.relative(RelativeDirection.RIGHT.getActualFacing(front), x)
                .relative(RelativeDirection.UP.getActualFacing(front), y)
                .relative(RelativeDirection.BACK.getActualFacing(front), z);
    }

    private static BlockPos firstUnloaded(FixedPatternChunkRetry.Bounds bounds, BlockPos center,
                                          Direction front, Set<Long> loaded) {
        for (int z = bounds.minZ(); z <= bounds.maxZ(); z++) {
            for (int y = bounds.minY(); y <= bounds.maxY(); y++) {
                for (int x = bounds.minX(); x <= bounds.maxX(); x++) {
                    BlockPos pos = worldPosition(center, front, x, y, z);
                    if (!loaded.contains(new ChunkPos(pos).toLong())) return pos;
                }
            }
        }
        return null;
    }

    private static final class LoadedChunks {
        private final Set<Long> loaded = new HashSet<>();
        private int reads;
        private BlockPos last;
        private boolean isLoaded(BlockPos pos) {
            reads++;
            last = pos.immutable();
            return loaded.contains(new ChunkPos(pos).toLong());
        }
        private void load(BlockPos pos) { loaded.add(new ChunkPos(pos).toLong()); }
        private void unload(BlockPos pos) { loaded.remove(new ChunkPos(pos).toLong()); }
    }
}
