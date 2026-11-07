package DIV.gtcsolo.audit;

import DIV.gtcsolo.performance.PatternPredicateLoop;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.pattern.MultiblockState;
import com.gregtechceu.gtceu.api.pattern.TraceabilityPredicate;
import com.gregtechceu.gtceu.api.pattern.error.PatternStringError;
import com.gregtechceu.gtceu.api.pattern.error.SinglePredicateError;
import com.gregtechceu.gtceu.api.pattern.predicates.SimplePredicate;
import net.minecraft.core.BlockPos;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

/** Actual native predicate/counter/error behavior versus the opt-in stream-free wrapper. */
public final class PatternPredicateLoopRegressionTest {
    private interface Engine { boolean test(TraceabilityPredicate predicate, MemoryState state); }
    private static final Engine NATIVE = TraceabilityPredicate::test;
    private static final Engine FAST = PatternPredicateLoop::test;

    public static void main(String[] args) throws Exception {
        if (!PatternPredicateLoopRegressionTest.class.desiredAssertionStatus()) throw new IllegalStateException("Use -ea");
        int compared = 0;
        for (int limited = 0; limited < 8; limited++) {
            for (int common = 0; common < 8; common++) {
                int limitedMask = limited, commonMask = common;
                compare(engine -> scenario(limitedMask, commonMask), 4);
                compared += 4;
            }
        }
        compare(engine -> recursive(engine, 20, false), 2);
        compare(engine -> recursive(engine, 20, true), 2);
        compare(engine -> mutatingCommon(true), 1);
        compare(engine -> mutatingCommon(false), 1);
        compare(engine -> new TraceabilityPredicate() {
            @Override public boolean test(MultiblockState state) {
                state.io = IO.OUT;
                ((MemoryState) state).trace.add("overridden-test");
                return false;
            }
        }, 1);
        compare(engine -> {
            var predicate = new TraceabilityPredicate();
            predicate.common = new ArrayList<>() {
                @Override public Stream<SimplePredicate> stream() { return Stream.empty(); }
            };
            predicate.common.add(simple("must-not-evaluate", state -> true));
            return predicate;
        }, 1);
        compare(engine -> replacingCommon(), 1);
        assertNoRetainedStates();

        AtomicReference<Throwable> failed = new AtomicReference<>();
        Thread worker = new Thread(() -> {
            try {
                compare(engine -> recursive(engine, 12, true), 2);
                compare(engine -> scenario(0, 5), 4);
                assertNoRetainedStates();
            } catch (Throwable failure) { failed.set(failure); }
        }, "predicate-loop-regression-worker");
        worker.start(); worker.join(5000);
        assert !worker.isAlive();
        if (failed.get() != null) throw new AssertionError("worker", failed.get());
        assertNoRetainedStates();
        System.out.println("Predicate loop passed " + compared + " truth-mask/limit rounds plus reentrancy,"
                + " exception and ArrayList fail-fast cases, custom predicate/list fallback, IO/error/render/slot context,"
                + " worker isolation and cleared bounded pooled state references.");
    }

    private static TraceabilityPredicate scenario(int limitedMask, int commonMask) {
        var result = new TraceabilityPredicate();
        for (int i = 0; i < 3; i++) {
            boolean matches = (limitedMask & (1 << i)) != 0;
            SimplePredicate predicate = simple("limited-" + i, state -> matches);
            predicate.maxCount = 2;
            predicate.maxLayerCount = 3;
            result.limited.add(predicate);
        }
        for (int i = 0; i < 3; i++) {
            boolean matches = (commonMask & (1 << i)) != 0;
            result.common.add(simple("common-" + i, state -> matches));
        }
        return result;
    }

    private static SimplePredicate simple(String label, Predicate<MemoryState> test) {
        var result = new SimplePredicate(label, state -> {
            var memory = (MemoryState) state;
            memory.trace.add(label);
            state.getMatchContext().increment(label, 1);
            state.setError(new PatternStringError("test." + label));
            return test.test(memory);
        }, null);
        result.io = (label.hashCode() & 1) == 0 ? IO.IN : IO.OUT;
        result.disableRenderFormed = true;
        result.slotName = label;
        return result;
    }

    private static TraceabilityPredicate recursive(Engine engine, int depth, boolean throwsAtLeaf) {
        var result = new TraceabilityPredicate();
        var child = depth == 0 ? null : recursive(engine, depth - 1, throwsAtLeaf);
        result.common.add(simple("recursive-" + depth, state -> {
            if (child != null) return engine.test(child, state);
            if (throwsAtLeaf) throw new IllegalArgumentException("test leaf failure");
            return true;
        }));
        result.common.add(simple("unreached-" + depth, state -> {
            throw new AssertionError("common predicates must short circuit");
        }));
        return result;
    }

    private static TraceabilityPredicate mutatingCommon(boolean returnsTrue) {
        var result = new TraceabilityPredicate();
        result.common.add(simple("mutating", state -> {
            result.common.add(simple("added-during-test", ignored -> false));
            return returnsTrue;
        }));
        return result;
    }

    private static TraceabilityPredicate replacingCommon() {
        var result = new TraceabilityPredicate();
        var limited = simple("replace-common", state -> {
            result.common = new ArrayList<>() {
                @Override public Stream<SimplePredicate> stream() {
                    state.getMatchContext().increment("foreign-stream", 1);
                    return Stream.empty();
                }
            };
            result.common.add(simple("foreign-must-not-evaluate", ignored -> true));
            return false;
        });
        limited.maxCount = 1;
        result.limited.add(limited);
        return result;
    }

    private static void compare(Function<Engine, TraceabilityPredicate> factory, int rounds) {
        List<Observation> nativeResults = run(NATIVE, factory.apply(NATIVE), rounds);
        List<Observation> fastResults = run(FAST, factory.apply(FAST), rounds);
        assert fastResults.equals(nativeResults) : "native=" + nativeResults + " fast=" + fastResults;
    }

    private static List<Observation> run(Engine engine, TraceabilityPredicate predicate, int rounds) {
        var state = new MemoryState();
        List<Observation> result = new ArrayList<>();
        for (int i = 0; i < rounds; i++) {
            Boolean matched = null;
            String thrown = "";
            state.trace.clear();
            try { matched = engine.test(predicate, state); }
            catch (RuntimeException failure) { thrown = failure.getClass().getName() + ":" + failure.getMessage(); }
            Map<String, String> context = new TreeMap<>();
            for (var entry : state.getMatchContext().entrySet()) context.put(entry.getKey(), String.valueOf(entry.getValue()));
            String error = state.error == null ? "" : state.error instanceof PatternStringError string ? string.translateKey :
                    state.error instanceof SinglePredicateError single ? single.predicate.type + ":" + single.type :
                            state.error.getClass().getName();
            result.add(new Observation(matched, thrown, List.copyOf(state.trace), state.io, error,
                    counts(state.getGlobalCount()), counts(state.getLayerCount()), context));
        }
        return result;
    }

    private static Map<String, Integer> counts(Map<SimplePredicate, Integer> values) {
        var result = new TreeMap<String, Integer>();
        values.forEach((predicate, count) -> result.merge(predicate.type, count, Integer::sum));
        return result;
    }

    private static void assertNoRetainedStates() throws Exception {
        Field threadLocal = PatternPredicateLoop.class.getDeclaredField("POOL");
        threadLocal.setAccessible(true);
        Object pool = ((ThreadLocal<?>) threadLocal.get(null)).get();
        Field idleField = pool.getClass().getDeclaredField("idle"); idleField.setAccessible(true);
        Deque<?> idle = (Deque<?>) idleField.get(pool);
        assert idle.size() <= 8;
        for (Object evaluation : idle) {
            Field state = evaluation.getClass().getDeclaredField("state"); state.setAccessible(true);
            assert state.get(evaluation) == null : "thread-local pools must not keep a world alive";
        }
    }

    private record Observation(Boolean matched, String thrown, List<String> trace, IO io, String error,
                               Map<String, Integer> global, Map<String, Integer> layer, Map<String, String> context) {}

    private static final class MemoryState extends MultiblockState {
        private final List<String> trace = new ArrayList<>();
        private MemoryState() {
            super(null, BlockPos.ZERO);
            clean();
            io = IO.NONE;
            setError(new PatternStringError("initial"));
        }
        @Override public BlockPos getPos() { return BlockPos.ZERO; }
    }
}
