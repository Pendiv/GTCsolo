package DIV.gtcsolo.machine.mega;

/** Balance constants and thermal arithmetic; keep tuning independent of UI and recipes. */
public final class MegaBlastRules {
    public static final int BATCH = 4096;
    public static final int COILS = 62797;
    public static final double EU_PER_K_PER_COIL = 500;
    public static final double IDLE_RETAIN_PER_SECOND = 0.9934;
    private MegaBlastRules() {}

    public static int normalTemperature(int coilTemperature) {
        return (int) Math.min(Integer.MAX_VALUE, Math.max(0L, (long) coilTemperature) * 3 / 2);
    }

    public static double euPerK(int coilDiscount) {
        return EU_PER_K_PER_COIL * COILS / Math.max(1, coilDiscount);
    }

    public static double sanitize(double heat) {
        return Double.isFinite(heat) ? Math.max(0, Math.min(Integer.MAX_VALUE, heat)) : 0;
    }

    public static double cool(double heat) {
        return sanitize(heat) * Math.pow(IDLE_RETAIN_PER_SECOND, 1.0 / 20);
    }

    public static long heatingEU(double heat, int target, int coilDiscount) {
        double cost = Math.max(0, (double) target - sanitize(heat)) * euPerK(coilDiscount);
        return (long) Math.ceil(Math.min(Long.MAX_VALUE, cost));
    }

    public static double heatAfterPayment(double heat, int target, int coilDiscount, long paid) {
        double initial = sanitize(heat);
        return Math.max(initial, Math.min(target, initial + Math.max(0, paid) / euPerK(coilDiscount)));
    }
}
