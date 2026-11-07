package DIV.gtcsolo.machine;

/** Arithmetic policy shared by the real recipe path and independent checks. */
public final class IndustrialCircuitRules {
    private IndustrialCircuitRules() {}
    public record Overclock(long eut, int duration, int stages) {}

    public static int ordinaryBonus(int modules, int stages) {
        if (modules < 1 || modules > 6 || stages < 0) throw new IllegalArgumentException("Invalid ICAL bonus");
        return Math.multiplyExact(8, Math.addExact(1 + modules, stages));
    }
    public static int extraLimit(int modules) {
        if (modules < 1 || modules > 6) throw new IllegalArgumentException("Invalid ICAL modules");
        return modules * 2;
    }
    public static Overclock overclock(long eut, int duration, long voltage) {
        if (eut < 0 || voltage < 0 || eut > voltage) return null;
        int stages = 0;
        long time = Math.max(1, duration);
        while (eut > 0 && eut <= voltage / 4) {
            eut *= 4;
            time = Math.max(1, time / 2);
            stages++;
        }
        return new Overclock(eut, (int) time, stages);
    }
}
