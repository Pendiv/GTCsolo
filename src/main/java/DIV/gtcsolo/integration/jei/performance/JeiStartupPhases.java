package DIV.gtcsolo.integration.jei.performance;

import DIV.gtcsolo.Gtcsolo;

import java.util.function.Supplier;

/** Measures the existing synchronous startup calls without changing their order or thread. */
public final class JeiStartupPhases {
    private JeiStartupPhases() {}

    public static void run(String phase, Runnable action) {
        measure(phase, () -> {
            action.run();
            return null;
        });
    }

    public static <T> T measure(String phase, Supplier<T> action) {
        if (!JeiOptimizationOptions.timingEnabled()) return action.get();
        long started = System.nanoTime();
        boolean completed = false;
        try {
            T result = action.get();
            completed = true;
            return result;
        } finally {
            Gtcsolo.LOGGER.info("[JEI-startup] {} completed={} durationMs={}",
                    phase, completed, (System.nanoTime() - started) / 1_000_000.0);
        }
    }
}
