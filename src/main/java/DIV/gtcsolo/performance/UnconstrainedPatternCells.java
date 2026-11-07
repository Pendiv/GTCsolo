package DIV.gtcsolo.performance;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.pattern.MultiblockState;
import com.gregtechceu.gtceu.api.pattern.TraceabilityPredicate;
import com.gregtechceu.gtceu.api.pattern.predicates.SimplePredicate;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.Map;
import java.util.function.Predicate;

/** A native, unmodified ANY cell has no relationship with blocks occupying that position. */
public final class UnconstrainedPatternCells {
    private static volatile boolean enabled = Boolean.parseBoolean(System.getProperty("gtcsolo.performance.unconstrainedCells", "true"));
    private static volatile Canonical canonical;

    private UnconstrainedPatternCells() {}

    /** Called at the end of native SimplePredicate initialization, before callers can mutate ANY. */
    public static synchronized void captureCanonicalAny(SimplePredicate any) {
        if (canonical == null) canonical = new Canonical(any, any.predicate);
    }

    public static void setEnabled(boolean value) { enabled = value; }
    public static boolean isEnabled() { return enabled; }
    public static Map<String, Object> snapshot() {
        return Map.of("enabled", enabled, "canonical_captured", canonical != null,
                "scope", "unmodified_native_ANY_cells_only");
    }

    public static boolean canSkip(MultiblockState state, TraceabilityPredicate predicate) {
        return enabled && state.getClass() == MultiblockState.class && isPureAny(predicate);
    }

    /** Identity plus neutral conditions prevents dropping extensions attached to the mutable ANY singleton. */
    public static boolean isPureAny(TraceabilityPredicate predicate) {
        Canonical captured = canonical;
        if (captured == null || predicate == null || predicate.getClass() != TraceabilityPredicate.class
                || predicate.isController || predicate.common == null || predicate.limited == null
                || predicate.common.getClass() != ArrayList.class || predicate.limited.getClass() != ArrayList.class
                || predicate.common.size() != 1 || !predicate.limited.isEmpty()
                || predicate.common.get(0) != captured.any()) return false;
        SimplePredicate any = captured.any();
        return any == SimplePredicate.ANY && any.getClass() == SimplePredicate.class
                && any.predicate == captured.predicate() && any.predicate != null
                && any.minCount == -1 && any.maxCount == -1 && any.minLayerCount == -1 && any.maxLayerCount == -1
                && any.previewCount == -1 && !any.disableRenderFormed && any.io == IO.BOTH
                && any.slotName == null && any.nbtParser == null && any.candidates == null && any.toolTips == null;
    }

    private record Canonical(SimplePredicate any, Predicate<MultiblockState> predicate) {}

    /** Implemented by a native state Mixin; no Level or controller is retained here. */
    public interface StateAccess {
        boolean gtcsolo$nativeUpdate(BlockPos position, TraceabilityPredicate predicate);
        void gtcsolo$updateUnconstrained(BlockPos position, TraceabilityPredicate predicate);
        void gtcsolo$clearUnconstrained();
        boolean gtcsolo$isUnconstrained();
    }
}
