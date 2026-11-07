package DIV.gtcsolo.machine.pump;

/** Paid water is additional to the primitive pump's unchanged passive production. */
public final class PumpAssistLogic {
    public static final int ELECTRIC_MULTIPLIER = 16;
    public static final int STEAM_MULTIPLIER = 8;
    public static final int ENERGY_PER_TICK = 16;
    public static final int STEAM_PER_TICK = 64;

    public enum Power { ELECTRIC, STEAM }

    public interface Ports {
        long storedEnergy();
        int storedSteam();
        int fillWater(int amount, boolean simulate);
        boolean consume(Power power, int amount);
        void refund(Power power, int amount);
    }

    private Power previousPower;
    private int waterRemainder;

    public void reset() {
        previousPower = null;
        waterRemainder = 0;
    }

    /** Runs once per server tick; blocked output never accumulates a catch-up batch. */
    public void tick(int passiveWaterPerSecond, Ports ports) {
        if (passiveWaterPerSecond <= 0) {
            reset();
            return;
        }
        Power power = ports.storedEnergy() >= ENERGY_PER_TICK ? Power.ELECTRIC
                : ports.storedSteam() >= STEAM_PER_TICK ? Power.STEAM : null;
        if (power == null) {
            reset();
            return;
        }
        if (power != previousPower) waterRemainder = 0;
        previousPower = power;
        int multiplier = power == Power.ELECTRIC ? ELECTRIC_MULTIPLIER : STEAM_MULTIPLIER;
        int fullCost = power == Power.ELECTRIC ? ENERGY_PER_TICK : STEAM_PER_TICK;
        long numerator = (long) passiveWaterPerSecond * (multiplier - 1) + waterRemainder;
        int requested = (int) Math.min(Integer.MAX_VALUE, numerator / 20);
        waterRemainder = (int) (numerator % 20);
        if (requested == 0) return;

        int accepted = Math.max(0, Math.min(requested, ports.fillWater(requested, true)));
        if (accepted == 0) {
            reset();
            return;
        }
        int cost = proportionalCost(fullCost, accepted, requested);
        if (!ports.consume(power, cost)) {
            reset();
            return;
        }
        int filled = Math.max(0, Math.min(accepted, ports.fillWater(accepted, false)));
        int actualCost = proportionalCost(fullCost, filled, requested);
        if (actualCost < cost) ports.refund(power, cost - actualCost);
        if (filled < requested) reset();
    }

    private static int proportionalCost(int fullCost, int water, int requested) {
        // Round up so repeated tiny openings cannot generate paid water for free.
        return (int) (((long) fullCost * water + requested - 1) / requested);
    }
}
