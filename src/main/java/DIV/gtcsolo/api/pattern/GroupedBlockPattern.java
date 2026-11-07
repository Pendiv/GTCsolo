package DIV.gtcsolo.api.pattern;

import com.gregtechceu.gtceu.api.pattern.BlockPattern;
import java.util.Objects;
import java.util.function.IntFunction;

/** Opt-in module repetition on a genuine GT BlockPattern, via GroupedBlockPatternMixin. */
public final class GroupedBlockPattern {
    private GroupedBlockPattern() {}

    public static BlockPattern repeat(int minimum, int maximum, IntFunction<BlockPattern> factory) {
        if (minimum < 1 || maximum < minimum) throw new IllegalArgumentException("Invalid module repetition range");
        Objects.requireNonNull(factory, "pattern factory");
        BlockPattern pattern = Objects.requireNonNull(factory.apply(minimum), "minimum pattern");
        if (!((Object) pattern instanceof GroupedPatternAccess access))
            throw new IllegalStateException("GroupedBlockPatternMixin has not been applied to GT BlockPattern");
        var plan = new GroupedPatternPlan(minimum, maximum, factory, pattern);
        access.gtcsolo$attachGroupedPattern(plan);
        return pattern;
    }
}
