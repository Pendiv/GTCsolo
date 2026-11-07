package DIV.gtcsolo.api.pattern;

import com.gregtechceu.gtceu.api.pattern.BlockPattern;
import com.gregtechceu.gtceu.api.pattern.MultiblockState;
import com.gregtechceu.gtceu.api.pattern.util.RelativeDirection;
import com.lowdragmc.lowdraglib.utils.BlockInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.IntFunction;

/** Chooses complete native patterns; contains no GT matching, IO or geometry implementation. */
public final class GroupedPatternPlan {
    private final int minimum, maximum;
    private final RelativeDirection[] axes;
    private final IntFunction<BlockPattern> factory;
    private final Map<Integer, BlockPattern> variants = new ConcurrentHashMap<>();
    private int formedCount;

    GroupedPatternPlan(int minimum, int maximum, IntFunction<BlockPattern> factory, BlockPattern smallest) {
        this.minimum = minimum;
        this.maximum = maximum;
        this.factory = factory;
        this.axes = smallest.structureDir.clone();
        validate(smallest);
    }

    public int minimum() { return minimum; }
    public int maximum() { return maximum; }
    public int formedCount() { return formedCount; }

    public void useNativeMinimum(BlockPattern pattern) {
        validate(pattern);
        if (variants.putIfAbsent(minimum, pattern) != null) throw new IllegalStateException("Module plan already attached");
    }

    private void validate(BlockPattern pattern) {
        if (pattern.getClass() != BlockPattern.class || !Arrays.equals(axes, pattern.structureDir)
                || (Object) pattern instanceof GroupedPatternAccess access && access.gtcsolo$hasGroupedPattern())
            throw new IllegalArgumentException("Module variants must be unhooked native patterns with matching axes");
        for (int[] repeat : pattern.aisleRepetitions) {
            if (repeat[0] < 1 || repeat[0] != repeat[1])
                throw new IllegalArgumentException("Expand modules into fixed aisles; do not nest variable repeats");
        }
        if (pattern.aisleRepetitions.length == 0) throw new IllegalArgumentException("Empty module pattern");
    }

    private BlockPattern variant(int count) {
        if (count < minimum || count > maximum) throw new IllegalArgumentException("Module count outside allowed range");
        return variants.computeIfAbsent(count, n -> {
            BlockPattern pattern = Objects.requireNonNull(factory.apply(n), "module pattern");
            validate(pattern);
            return pattern;
        });
    }

    public boolean match(MultiblockState state, boolean savePredicate) {
        formedCount = 0;
        for (int count = maximum; count >= minimum; count--) {
            if (variant(count).checkPatternAt(state, savePredicate)) {
                formedCount = count;
                return true;
            }
        }
        return false;
    }

    public boolean match(MultiblockState state, BlockPos center, Direction front,
                         Direction upwards, boolean flipped, boolean savePredicate) {
        formedCount = 0;
        for (int count = maximum; count >= minimum; count--) {
            if (variant(count).checkPatternAt(state, center, front, upwards, flipped, savePredicate)) {
                formedCount = count;
                return true;
            }
        }
        return false;
    }

    public BlockInfo[][][] preview(int[] repetition) {
        if (repetition == null || repetition.length != 1)
            throw new IllegalArgumentException("Specify one module repetition count");
        BlockPattern pattern = variant(repetition[0]);
        int[] fixed = Arrays.stream(pattern.aisleRepetitions).mapToInt(range -> range[0]).toArray();
        return pattern.getPreview(fixed);
    }

    public void autoBuild(Player player, MultiblockState state) {
        variant(formedCount >= minimum && formedCount <= maximum ? formedCount : minimum).autoBuild(player, state);
    }
}
