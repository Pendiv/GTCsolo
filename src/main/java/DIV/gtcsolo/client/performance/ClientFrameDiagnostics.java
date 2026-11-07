package DIV.gtcsolo.client.performance;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Explicit diagnostic windows only; owns primitive timings and at most 128 slow-frame records. */
public final class ClientFrameDiagnostics {
    public enum Phase {
        TICK("tick"), RENDER("render"), DISPLAY_UPDATE("display_update"),
        SWAP_BUFFERS("swap_buffers"), LIMIT("limit");

        final String key;
        Phase(String key) { this.key = key; }
    }

    private static final Recorder RECORDER = new Recorder(new SystemClock());

    private ClientFrameDiagnostics() {}

    /** Call while PerformanceDiagnostics is disabled, before starting a fresh diagnostic window. */
    public static void reset() { RECORDER.reset(); }
    public static Map<String, Object> snapshot() { return RECORDER.snapshot(); }
    public static void beginFrame() { RECORDER.beginFrame(); }
    public static void endFrame() { RECORDER.endFrame(); }
    public static void beginPhase(Phase phase) { RECORDER.beginPhase(phase); }
    public static void endPhase(Phase phase) { RECORDER.endPhase(phase); }

    interface Clock {
        long nanos();
        long cpuNanos();
        long unixMillis();
        boolean cpuAvailable();
    }

    private static final class SystemClock implements Clock {
        private final ThreadMXBean bean = findBean();
        private static ThreadMXBean findBean() {
            try {
                ThreadMXBean value = ManagementFactory.getThreadMXBean();
                return value.isCurrentThreadCpuTimeSupported() && value.isThreadCpuTimeEnabled() ? value : null;
            } catch (RuntimeException unavailable) {
                return null;
            }
        }
        @Override public long nanos() { return System.nanoTime(); }
        @Override public long unixMillis() { return System.currentTimeMillis(); }
        @Override public boolean cpuAvailable() { return bean != null; }
        @Override public long cpuNanos() {
            if (bean == null) return -1;
            try { return bean.getCurrentThreadCpuTime(); }
            catch (RuntimeException unavailable) { return -1; }
        }
    }

    static final class Recorder {
        private static final int CAPACITY = 128;
        private static final long THRESHOLD = 100_000_000L;
        private static final Phase[] PHASES = Phase.values();
        private final Clock clock;
        private final Object lock = new Object();
        private final ThreadLocal<Frame> current = ThreadLocal.withInitial(Frame::new);
        private final ArrayDeque<Sample> slow = new ArrayDeque<>(CAPACITY);
        private volatile long generation;
        private long completedFrames, outlierFrames, abandonedFrames;

        Recorder(Clock clock) { this.clock = clock; }

        void reset() {
            synchronized (lock) {
                generation++;
                slow.clear();
                completedFrames = outlierFrames = abandonedFrames = 0;
            }
        }

        void beginFrame() {
            Frame frame = current.get();
            long epoch = generation;
            if (frame.active && frame.generation == epoch) {
                synchronized (lock) { if (generation == epoch) abandonedFrames++; }
            }
            if (frame.generation != epoch) {
                frame.previousEnd = 0;
                frame.previousCpuEnd = -1;
            }
            frame.generation = epoch;
            frame.active = true;
            frame.started = clock.nanos();
            frame.cpuStarted = clock.cpuNanos();
            Arrays.fill(frame.wall, 0);
            Arrays.fill(frame.cpu, 0);
            Arrays.fill(frame.depth, 0);
            Arrays.fill(frame.calls, 0);
            Arrays.fill(frame.cpuValid, true);
        }

        void beginPhase(Phase phase) {
            Frame frame = current.get();
            if (!frame.active || frame.generation != generation) return;
            int i = phase.ordinal();
            frame.calls[i]++;
            if (frame.depth[i]++ == 0) {
                frame.phaseStart[i] = clock.nanos();
                frame.phaseCpuStart[i] = clock.cpuNanos();
            }
        }

        void endPhase(Phase phase) {
            Frame frame = current.get();
            if (!frame.active || frame.generation != generation) return;
            int i = phase.ordinal();
            if (frame.depth[i] == 0 || --frame.depth[i] != 0) return;
            long cpu = clock.cpuNanos();
            long wall = clock.nanos();
            frame.wall[i] += Math.max(0, wall - frame.phaseStart[i]);
            long elapsedCpu = elapsedCpu(frame.phaseCpuStart[i], cpu);
            if (elapsedCpu < 0) frame.cpuValid[i] = false;
            else frame.cpu[i] += elapsedCpu;
        }

        void endFrame() {
            Frame frame = current.get();
            if (!frame.active) return;
            frame.active = false;
            long cpuEnd = clock.cpuNanos();
            long end = clock.nanos();
            long wall = Math.max(0, end - frame.started);
            long cpu = elapsedCpu(frame.cpuStarted, cpuEnd);
            long gap = frame.previousEnd == 0 ? 0 : Math.max(0, frame.started - frame.previousEnd);
            long gapCpu = frame.previousEnd == 0 ? 0 : elapsedCpu(frame.previousCpuEnd, frame.cpuStarted);
            long interval = wall + gap;
            boolean outlier = interval >= THRESHOLD;
            // Snapshot primitive arrays only for outliers; ordinary frames allocate no record or map.
            Sample sample = outlier ? new Sample(end, clock.unixMillis(), wall, cpu, gap, gapCpu,
                    frame.wall.clone(), frame.cpu.clone(), frame.calls.clone(), frame.depth.clone(),
                    frame.cpuValid.clone()) : null;
            synchronized (lock) {
                if (frame.generation != generation) return;
                completedFrames++;
                if (outlier) {
                    outlierFrames++;
                    if (slow.size() == CAPACITY) slow.removeFirst();
                    slow.addLast(sample);
                }
            }
            frame.previousEnd = end;
            frame.previousCpuEnd = cpuEnd;
        }

        Map<String, Object> snapshot() {
            synchronized (lock) {
                var result = new LinkedHashMap<String, Object>();
                result.put("threshold_ms", THRESHOLD / 1_000_000.0);
                result.put("capacity", CAPACITY);
                result.put("completed_frames", completedFrames);
                result.put("outlier_frames", outlierFrames);
                result.put("abandoned_frames", abandonedFrames);
                result.put("cpu_time_available", clock.cpuAvailable());
                result.put("cpu_clock", "ThreadMXBean.getCurrentThreadCpuTime");
                result.put("phase_note", "render includes clear through blit; display_update includes swap_buffers; other excludes this nested swap phase");
                List<Map<String, Object>> rows = new ArrayList<>(slow.size());
                for (Sample sample : slow) rows.add(sample.toMap());
                result.put("slow_frames", rows);
                return result;
            }
        }

        private static long elapsedCpu(long start, long end) {
            return start < 0 || end < start ? -1 : end - start;
        }

        private static Double millis(long nanos) { return nanos < 0 ? null : nanos / 1_000_000.0; }

        private static final class Frame {
            boolean active;
            long generation = -1, started, cpuStarted, previousEnd, previousCpuEnd = -1;
            final long[] wall = new long[PHASES.length], cpu = new long[PHASES.length];
            final long[] phaseStart = new long[PHASES.length], phaseCpuStart = new long[PHASES.length];
            final int[] depth = new int[PHASES.length], calls = new int[PHASES.length];
            final boolean[] cpuValid = new boolean[PHASES.length];
        }

        private record Sample(long endNanos, long endUnixMillis, long wall, long cpu, long gap, long gapCpu,
                              long[] phaseWall, long[] phaseCpu, int[] calls, int[] unfinished, boolean[] cpuValid) {
            Map<String, Object> toMap() {
                var row = new LinkedHashMap<String, Object>();
                row.put("end_unix_ms", endUnixMillis);
                row.put("start_unix_ms", endUnixMillis - wall / 1_000_000.0);
                row.put("end_nano_time", endNanos);
                row.put("duration_ms", millis(wall));
                row.put("cpu_ms", millis(cpu));
                row.put("frame_interval_ms", millis(wall + gap));
                row.put("gap_before_ms", millis(gap));
                row.put("gap_before_cpu_ms", millis(gapCpu));
                var phases = new LinkedHashMap<String, Object>();
                List<String> incomplete = new ArrayList<>();
                long accountedWall = 0, accountedCpu = 0;
                boolean accountedCpuValid = cpu >= 0;
                for (Phase phase : PHASES) {
                    int i = phase.ordinal();
                    var timing = new LinkedHashMap<String, Object>();
                    timing.put("calls", calls[i]);
                    timing.put("wall_ms", millis(phaseWall[i]));
                    timing.put("cpu_ms", cpuValid[i] ? millis(phaseCpu[i]) : null);
                    phases.put(phase.key, timing);
                    if (unfinished[i] != 0) incomplete.add(phase.key);
                    if (phase != Phase.SWAP_BUFFERS) {
                        accountedWall += phaseWall[i];
                        accountedCpu += phaseCpu[i];
                        accountedCpuValid &= cpuValid[i];
                    }
                }
                row.put("phases", phases);
                row.put("other_wall_ms", millis(Math.max(0, wall - accountedWall)));
                row.put("other_cpu_ms", accountedCpuValid ? millis(Math.max(0, cpu - accountedCpu)) : null);
                row.put("unfinished_phases", incomplete);
                return row;
            }
        }
    }
}
