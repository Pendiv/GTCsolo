package DIV.gtcsolo.machine;

/** Pure room balance rules. Free IO bonuses never represent electrical parallels. */
public final class RoomRecipeRules {
    public static final String REQUIRED_TEMPERATURE = "gtcsolo_room_temperature";
    public static final String PAID_PARALLELS = "gtcsolo_room_paid_parallels";
    public static final String SUBTICK_PARALLELS = "gtcsolo_room_subtick_parallels";
    public static final String IO_BONUS = "gtcsolo_room_io_bonus";
    public static final String HEAT_STEPS = "gtcsolo_room_heat_steps";
    public static final double NORMAL_DURATION = 0.9;
    public static final double HEATED_DURATION = 0.85;
    public static final int HEAT_STEP = 900;

    private RoomRecipeRules() {}

    public record Profile(long eut, int duration, int stages, int heatSteps,
                          int heatedStages, int ioBonus, int subtickParallels) {}

    public static int heatSteps(boolean construction, int temperature, int required) {
        if (!construction) return 0;
        if (required < 0 || temperature < required) return -1;
        return (int) (((long) temperature - required) / HEAT_STEP);
    }

    public static int maximumStages(long eut, long voltage) {
        if (eut <= 0 || eut > voltage) return -1;
        int count = 0;
        while (eut <= voltage / 4) {
            eut *= 4;
            count++;
        }
        return count;
    }

    /**
     * Like GT sub-tick OC, freeze the last duration above one tick and convert
     * later speed factors into additional recipe rolls. Integer rolls round down.
     * The industrial IO doubling still applies to every accepted electrical OC.
     */
    public static Profile profile(long eut, int ticks, int stages, boolean construction,
                                  int temperature, int required) {
        int heat = heatSteps(construction, temperature, required);
        if (eut <= 0 || ticks <= 0 || stages < 0 || stages > 30 || heat < 0) return null;
        int heated = Math.min(stages, heat);
        long io = construction ? 4L + 2L * heat : 1L;
        double duration = ticks;
        double subtick = 1;
        boolean belowTick = false;
        for (int stage = 0; stage < stages; stage++) {
            if (eut > Long.MAX_VALUE / 4 || io > Integer.MAX_VALUE / 2) return null;
            eut *= 4;
            io *= 2;
            double factor = stage < heated ? HEATED_DURATION : NORMAL_DURATION;
            if (belowTick || duration * factor < 1) {
                subtick /= factor;
                belowTick = true;
            } else {
                duration *= factor;
            }
        }
        if (io > Integer.MAX_VALUE || subtick >= Integer.MAX_VALUE) return null;
        return new Profile(eut, Math.max(1, (int) duration), stages, heat, heated,
                (int) io, Math.max(1, (int) subtick));
    }
}
