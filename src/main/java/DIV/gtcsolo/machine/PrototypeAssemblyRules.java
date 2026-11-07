package DIV.gtcsolo.machine;

/** Probability arithmetic uses basis points, so 7.4% and 4.4% accumulate exactly. */
public final class PrototypeAssemblyRules {
    public static final int CERTAIN = 10_000;
    private PrototypeAssemblyRules() {}

    public static int failureGain(int recipeTier, int hatchTier) {
        return switch (recipeTier - hatchTier) {
            case 0 -> 3300;
            case 1 -> 740;
            case 2 -> 440;
            default -> 0;
        };
    }

    public static int successChance(int accumulated, int recipeTier, int hatchTier) {
        if (recipeTier >= 0 && hatchTier > recipeTier) return CERTAIN;
        return Math.max(0, Math.min(CERTAIN, accumulated));
    }

    public static int afterFailure(int accumulated, int recipeTier, int hatchTier) {
        return Math.min(CERTAIN, Math.max(0, accumulated) + failureGain(recipeTier, hatchTier));
    }

    public static boolean canRun(int recipeTier, int hatchTier) {
        return hatchTier >= 0 && recipeTier >= 0 && recipeTier <= hatchTier + 2;
    }
}
