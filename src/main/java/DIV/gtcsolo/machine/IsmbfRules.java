package DIV.gtcsolo.machine;

/** Pure ISMBF arithmetic, independent of world state and recipe inventories. */
public final class IsmbfRules {
    public static final String START_TEMPERATURE = "gtcsolo_ismbf_start_temperature";
    public static final double MAX_HEALTH = 100_000;
    public static final double EU_PER_HEAT = 500;
    public static final double AMBIENT = 0;
    public static final double REPAIR_PER_SECOND = MAX_HEALTH * 0.01;
    public static final float EXPLOSION_STRENGTH = 15;
    public static final int EXPLOSION_COUNT = 3;

    private IsmbfRules() {}

    public static boolean canStartExtreme(int coilTemperature, int minimumCoilTemperature, double storedHeat) {
        return coilTemperature >= Math.max(0, minimumCoilTemperature) && storedHeat + 1e-7 >= coilTemperature;
    }

    public static long heatingEU(double heat, double target) {
        return (long) Math.ceil(Math.max(0, target - heat) * EU_PER_HEAT);
    }

    public static double heatAfterPayment(double heat, double target, long paidEU) {
        return Math.max(heat, Math.min(target, heat + Math.max(0, paidEU) / EU_PER_HEAT));
    }

    public static boolean canProcessExtreme(int coilTemperature, int minimumCoilTemperature,
                                            double heat, int operatingTemperature) {
        return operatingTemperature > 0 && canStartExtreme(coilTemperature, minimumCoilTemperature, heat)
                && heat + 1e-7 >= operatingTemperature;
    }

    public static int smeltingBonus(double heat, boolean alone) {
        int factor = !alone ? 1 : heat > 14000 ? 64 : heat > 12000 ? 16 : heat > 10000 ? 4 : 1;
        return 4096 * factor;
    }

    public static double cool(double heat) {
        return cool(heat, 1);
    }

    public static double cool(double heat, int ticks) {
        if (ticks <= 0) return heat;
        double cooled = Math.max(0, heat) * Math.pow(0.93, ticks / 20.0);
        // Exponential cooling otherwise never reaches ambient exactly.
        return cooled < 1 ? AMBIENT : cooled;
    }

    public static double durationBonus(double heat, boolean alloy) {
        if (!alloy) return heat > 10000 ? 0.75 : 1;
        return heat > 14000 ? 0.66 : heat > 12000 ? 0.75 : heat > 10000 ? 0.8 : 1;
    }

    public static double convertedDiscount(int requiredHeat, double heat, boolean alloy) {
        int steps = Math.max(0, (int) ((heat - requiredHeat) / 900));
        double discount = Math.pow(0.95, steps);
        return alloy ? discount * discount : discount;
    }

    public static double coolingPerTick(double operatingHeat) { return Math.max(0, operatingHeat) / 20; }

    public static double healthAfterCooling(double health, double unmetCooling) {
        return Math.max(0, Math.min(MAX_HEALTH, health) - Math.max(0, unmetCooling));
    }

    public record Overclock(long eut, int duration, int parallels, int stages, long lastTickEU) {}

    public static Overclock overclock(long eut, double ticks, long voltage, int perfectStages,
                                     boolean subtick, int parallelLimit) {
        if (eut <= 0 || eut > voltage || !Double.isFinite(ticks) || ticks <= 0 || parallelLimit < 1) return null;
        int stages = 0;
        double duration = ticks;
        long power = eut;
        while (power <= voltage / 4) {
            double next = duration / (stages < perfectStages ? 4 : 2);
            if (!subtick && next < 1) break;
            if (subtick && next < 1 && Math.floor(1 / next) > parallelLimit) break;
            power *= 4;
            duration = next;
            stages++;
        }
        int parallel = subtick && duration < 1 ? (int) Math.min(parallelLimit, Math.floor(1 / duration)) : 1;
        parallel = Math.max(1, parallel);
        double effectiveTicks = duration * parallel;
        int ticksRounded = (int) Math.max(1, Math.ceil(effectiveTicks));
        double finalFraction = effectiveTicks - (ticksRounded - 1.0);
        long finalEU = Math.max(1, Math.min(power, (long) Math.ceil(power * finalFraction)));
        return new Overclock(power, ticksRounded, parallel, stages, finalEU);
    }
}
