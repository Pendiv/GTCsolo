package DIV.gtcsolo.performance;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Predicate;

/**
 * Negative proof for a fixed, non-flipped RIGHT/UP/BACK structure: a position
 * reached by native checking remains mandatory while that position is unloaded.
 * It stores coordinates and orientation only, never blocks, predicates or a level.
 */
public final class FixedPatternChunkRetry {
    private static volatile boolean enabled = Boolean.parseBoolean(System.getProperty("gtcsolo.performance.chunkRetry", "true"));
    private static final LongAdder REMEMBERED = new LongAdder();
    private static final LongAdder SKIPPED = new LongAdder();
    private static final LongAdder INVALIDATED = new LongAdder();
    private final Bounds bounds;
    private Failure failure;

    public FixedPatternChunkRetry(Bounds bounds) { this.bounds = bounds; }

    public record Bounds(int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {
        public Bounds {
            if (minX > maxX || minY > maxY || minZ > maxZ) throw new IllegalArgumentException("Empty fixed pattern");
        }
        public boolean contains(BlockPos center, Direction front, BlockPos position) {
            int dx = position.getX() - center.getX();
            int dy = position.getY() - center.getY();
            int dz = position.getZ() - center.getZ();
            Direction right = front.getClockWise();
            Direction back = front.getOpposite();
            int x = dx * right.getStepX() + dz * right.getStepZ();
            int z = dx * back.getStepX() + dz * back.getStepZ();
            return x >= minX && x <= maxX && dy >= minY && dy <= maxY && z >= minZ && z <= maxZ;
        }
    }

    private record Failure(ResourceLocation dimension, BlockPos center, Direction front, Direction up,
                           boolean flipped, BlockPos requiredPosition) {}

    public static void setEnabled(boolean value) { enabled = value; }
    public static boolean isEnabled() { return enabled; }

    public void clear() { failure = null; }

    /** Called only after a real native UNLOAD_ERROR, never after a failed ordinary predicate. */
    public void rememberUnloaded(ResourceLocation dimension, BlockPos center, Direction front, Direction up,
                                 boolean flipped, BlockPos requiredPosition, Predicate<BlockPos> isLoaded) {
        failure = null;
        if (!enabled || !supported(front, up, flipped) || requiredPosition == null
                || !bounds.contains(center, front, requiredPosition) || isLoaded.test(requiredPosition)) return;
        failure = new Failure(dimension, center.immutable(), front, up, flipped, requiredPosition.immutable());
        REMEMBERED.increment();
    }

    public boolean shouldSkip(ResourceLocation dimension, BlockPos center, Direction front, Direction up,
                              boolean flipped, Predicate<BlockPos> isLoaded) {
        Failure previous = failure;
        if (!enabled || previous == null) return false;
        if (!supported(front, up, flipped) || !previous.dimension().equals(dimension)
                || !previous.center().equals(center) || previous.front() != front || previous.up() != up
                || previous.flipped() != flipped || isLoaded.test(previous.requiredPosition())) {
            failure = null;
            INVALIDATED.increment();
            return false;
        }
        SKIPPED.increment();
        return true;
    }

    private static boolean supported(Direction front, Direction up, boolean flipped) {
        return front != null && front.getAxis().isHorizontal() && up == Direction.NORTH && !flipped;
    }

    public static Map<String, Object> snapshot() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("enabled", enabled);
        result.put("remembered_native_unloads", REMEMBERED.sum());
        result.put("skipped_unloaded_retries", SKIPPED.sum());
        result.put("invalidated_proofs", INVALIDATED.sum());
        return result;
    }

    public static void resetCounters() {
        REMEMBERED.reset();
        SKIPPED.reset();
        INVALIDATED.reset();
    }
}
