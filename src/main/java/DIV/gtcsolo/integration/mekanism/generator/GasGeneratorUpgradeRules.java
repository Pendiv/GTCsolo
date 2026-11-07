package DIV.gtcsolo.integration.mekanism.generator;

/** The gas generator's upgrade policy, separate from Mekanism's machine-wide settings. */
public final class GasGeneratorUpgradeRules {
    public static final double MAX_SPEED_MULTIPLIER = 10.0;

    private GasGeneratorUpgradeRules() {}

    public static double speedMultiplier(int installed, int maximum) {
        if (maximum <= 0) return 1;
        int count = Math.max(0, Math.min(installed, maximum));
        return Math.pow(MAX_SPEED_MULTIPLIER, count / (double) maximum);
    }

    public static double capacityMultiplier(int installed) {
        return Math.pow(3.0, Math.max(0, installed) / 2.0);
    }

    /** Scale the native integer burn budget before its fuel and free-storage clamps. */
    public static double burnLimit(double unroundedNativeLimit, int installed, int maximum) {
        return Math.ceil(Math.ceil(unroundedNativeLimit) * speedMultiplier(installed, maximum));
    }
}
