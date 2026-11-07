package DIV.gtcsolo.performance;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.pattern.MultiblockState;
import com.gregtechceu.gtceu.api.pattern.TraceabilityPredicate;
import com.gregtechceu.gtceu.api.pattern.predicates.SimplePredicate;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Map;
import java.util.function.Consumer;

/** Removes Stream/MatchOps wrappers while retaining the native spliterator's order and fail-fast checks. */
public final class PatternPredicateLoop {
    private static volatile boolean enabled = Boolean.parseBoolean(System.getProperty("gtcsolo.performance.predicateLoops", "true"));
    private static final ThreadLocal<Pool> POOL = ThreadLocal.withInitial(Pool::new);
    private PatternPredicateLoop() {}

    public static void setEnabled(boolean value) { enabled = value; }
    public static boolean isEnabled() { return enabled; }
    public static Map<String, Object> snapshot() {
        return Map.of("enabled", enabled, "scope", "native_pattern_standard_traceability_predicates");
    }

    private static boolean supports(TraceabilityPredicate predicate) {
        return predicate.getClass() == TraceabilityPredicate.class && predicate.common != null && predicate.limited != null
                && predicate.common.getClass() == ArrayList.class && predicate.limited.getClass() == ArrayList.class;
    }

    public static boolean test(TraceabilityPredicate predicate, MultiblockState state) {
        if (!supports(predicate)) return predicate.test(state);
        state.io = IO.BOTH;
        boolean matched = false;
        // Every limited predicate is evaluated, even after an earlier one succeeds.
        for (SimplePredicate limited : predicate.limited) if (limited.testLimited(state)) matched = true;
        if (!matched) {
            // A custom predicate may replace the public common list while limited predicates run.
            if (predicate.common.getClass() != ArrayList.class) {
                matched = predicate.common.stream().anyMatch(common -> common.test(state));
                if (matched) state.setError(null);
                return matched;
            }
            Pool pool = POOL.get();
            Evaluation evaluation = pool.acquire(state);
            try {
                var iterator = predicate.common.spliterator();
                while (iterator.tryAdvance(evaluation)) {
                    if (evaluation.matched) {
                        matched = true;
                        break;
                    }
                }
            } finally {
                pool.release(evaluation);
            }
        }
        if (matched) state.setError(null);
        return matched;
    }

    private static final class Evaluation implements Consumer<SimplePredicate> {
        private MultiblockState state;
        private boolean matched;
        @Override public void accept(SimplePredicate predicate) { matched = predicate.test(state); }
    }

    private static final class Pool {
        private static final int MAX_IDLE = 8;
        private final ArrayDeque<Evaluation> idle = new ArrayDeque<>();
        private Evaluation acquire(MultiblockState state) {
            Evaluation result = idle.pollLast();
            if (result == null) result = new Evaluation();
            result.state = state;
            result.matched = false;
            return result;
        }
        private void release(Evaluation evaluation) {
            evaluation.state = null;
            evaluation.matched = false;
            if (idle.size() < MAX_IDLE) idle.addLast(evaluation);
        }
    }
}
