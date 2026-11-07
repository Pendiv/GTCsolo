package DIV.gtcsolo.performance;

import com.gregtechceu.gtceu.api.pattern.MultiblockState;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController;
import com.gregtechceu.gtceu.api.pattern.error.PatternStringError;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/** Bounded controller timing data; never retains a level, controller, pattern or match result. */
public final class PatternCheckProfiler {
    private static final int MAX_CONTROLLERS = 4096;
    private static final Object WINDOW_LOCK = new Object();
    private static final ConcurrentHashMap<Key, Counters> CONTROLLERS = new ConcurrentHashMap<>();
    private static final ThreadLocal<Boolean> INSIDE_CHECK = ThreadLocal.withInitial(() -> false);
    private static final LongAdder DROPPED = new LongAdder();
    private static volatile long resetNanos = System.nanoTime();
    private static volatile boolean enabled;

    private PatternCheckProfiler() {}

    /** Zero means a client/preview check or a nested grouped-pattern variant. */
    public static long begin(MultiblockState state) {
        if (!enabled || !(state.world instanceof ServerLevel) || INSIDE_CHECK.get()) return 0;
        synchronized (WINDOW_LOCK) {
            if (!enabled) return 0;
            INSIDE_CHECK.set(true);
            return System.nanoTime();
        }
    }

    /** Called in finally, including failed predicates and exceptions. Does not change GT's state. */
    public static void finish(IMultiController controller, MultiblockState state, long started, boolean matched, boolean threw) {
        if (started == 0) return;
        long elapsed = Math.max(0, System.nanoTime() - started);
        try {
            synchronized (WINDOW_LOCK) {
            // A native check can outlive stop/reset. Never attribute its old work to a new window.
            if (!enabled || started < resetNanos) return;
            String machine = controller.self().getDefinition().getId().toString();
            var pos = state.controllerPos;
            Key key = new Key(state.world.dimension().location().toString(), pos.getX(), pos.getY(), pos.getZ(), machine);
            Counters counters = CONTROLLERS.get(key);
            if (counters == null) {
                if (CONTROLLERS.size() >= MAX_CONTROLLERS) {
                    DROPPED.increment();
                    return;
                }
                counters = CONTROLLERS.computeIfAbsent(key, ignored -> new Counters());
            }
            int ioEntries = state.getMatchContext().getOrDefault("ioMap", Map.of()).size();
            Map<String, Object> detail = new LinkedHashMap<>();
            detail.put("error_key", state.error instanceof PatternStringError error ? error.translateKey : "");
            detail.put("controller_class", controller.getClass().getName());
            detail.put("formed", controller.isFormed());
            var front = controller.self().getFrontFacing();
            var up = controller.self().getUpwardsFacing();
            detail.put("front", front == null ? "none" : front.getName());
            detail.put("up", up == null ? "none" : up.getName());
            detail.put("flipped", controller.self().isFlipped());
            try {
                var checked = state.getPos();
                detail.put("position", List.of(checked.getX(), checked.getY(), checked.getZ()));
            } catch (NullPointerException neverReachedFirstPosition) {
                detail.put("position", List.of());
            }
            counters.add(elapsed, matched, threw, ioEntries, state.cache == null ? 0 : state.cache.size(),
                    Thread.currentThread().getName(), state.error == null ? "" : state.error.getClass().getSimpleName(), detail);
            }
        } catch (RuntimeException diagnosticFailure) {
            // Diagnostics must never turn a successful native match into an exception.
            synchronized (WINDOW_LOCK) {
                if (enabled && started >= resetNanos) DROPPED.increment();
            }
        } finally {
            INSIDE_CHECK.remove();
        }
    }

    public static void reset() {
        synchronized (WINDOW_LOCK) {
            CONTROLLERS.clear();
            DROPPED.reset();
            resetNanos = System.nanoTime();
        }
    }

    public static void setEnabled(boolean value) {
        synchronized (WINDOW_LOCK) { enabled = value; }
    }
    public static boolean isEnabled() { return enabled; }

    /** Plain maps make this directly serializable by the runtime diagnostic command. */
    public static Map<String, Object> snapshot() {
        synchronized (WINDOW_LOCK) {
        List<Map<String, Object>> rows = new ArrayList<>();
        CONTROLLERS.forEach((key, value) -> rows.add(value.snapshot(key)));
        rows.sort(Comparator.comparingDouble(row -> -((Number) row.get("total_ms")).doubleValue()));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("enabled", enabled);
        result.put("window_seconds", Math.max(0, System.nanoTime() - resetNanos) / 1_000_000_000.0);
        result.put("scope", "native_async_check_pattern_with_try_lock");
        result.put("dropped_checks", DROPPED.sum());
        result.put("controllers", List.copyOf(rows));
        return result;
        }
    }

    private record Key(String dimension, int x, int y, int z, String machine) {}

    private static final class Counters {
        private final LongAdder checks = new LongAdder();
        private final LongAdder successes = new LongAdder();
        private final LongAdder failures = new LongAdder();
        private final LongAdder exceptions = new LongAdder();
        private final LongAdder totalNanos = new LongAdder();
        private final AtomicLong maxNanos = new AtomicLong();
        private volatile long lastNanos;
        private volatile int lastIoEntries;
        private volatile int lastCachedPositions;
        private volatile String lastThread = "";
        private volatile String lastError = "";
        private volatile Map<String, Object> lastDetail = Map.of();

        private void add(long nanos, boolean matched, boolean threw, int ioEntries, int cachedPositions,
                         String thread, String error, Map<String, Object> detail) {
            checks.increment();
            if (matched) successes.increment(); else failures.increment();
            if (threw) exceptions.increment();
            totalNanos.add(nanos);
            maxNanos.accumulateAndGet(nanos, Math::max);
            lastNanos = nanos;
            lastIoEntries = ioEntries;
            lastCachedPositions = cachedPositions;
            lastThread = thread;
            lastError = error;
            lastDetail = detail;
        }

        private Map<String, Object> snapshot(Key key) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("machine", key.machine());
            row.put("dimension", key.dimension());
            row.put("x", key.x());
            row.put("y", key.y());
            row.put("z", key.z());
            row.put("checks", checks.sum());
            row.put("successes", successes.sum());
            row.put("failures", failures.sum());
            row.put("exceptions", exceptions.sum());
            row.put("total_ms", totalNanos.sum() / 1_000_000.0);
            row.put("max_ms", maxNanos.get() / 1_000_000.0);
            row.put("last_ms", lastNanos / 1_000_000.0);
            row.put("last_io_entries", lastIoEntries);
            row.put("last_cached_positions", lastCachedPositions);
            row.put("last_thread", lastThread);
            row.put("last_error", lastError);
            row.put("last_detail", lastDetail);
            return row;
        }
    }
}
