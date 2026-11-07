package DIV.gtcsolo.machine;

public final class BiochemicalProcessRules {
    public static final String LOAD = "gtcsolo_process_load";
    public static final String MINIMUM_TICKS = "gtcsolo_residence_ticks";
    public static final int CAPACITY_PER_SECTION = 16000;

    private BiochemicalProcessRules() {}

    public static int capacity(int sections) {
        return Math.max(0, Math.min(4, sections)) * CAPACITY_PER_SECTION;
    }

    public static int limit(int sections, int load) {
        return load <= 0 ? 0 : capacity(sections) / load;
    }

    public static int duration(int ticks, int minimum, int overclocks) {
        if (ticks <= 0 || minimum <= 0 || minimum > ticks || overclocks < 0) return -1;
        return Math.max(minimum, (int) Math.ceil(ticks / Math.pow(2, Math.min(30, overclocks))));
    }
}
