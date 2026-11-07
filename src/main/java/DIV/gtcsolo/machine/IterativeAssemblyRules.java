package DIV.gtcsolo.machine;

import com.gregtechceu.gtceu.api.recipe.OverclockingLogic;

/** Each stage costs 4x EU/t. Matching the output sample improves its speed factor. */
public final class IterativeAssemblyRules {
    public static final double NORMAL_SPEED = 1.8;
    public static final double MATCHED_SPEED = 2.25;
    private static final OverclockingLogic NORMAL = OverclockingLogic.create(1 / NORMAL_SPEED, 4, false);
    private static final OverclockingLogic MATCHED = OverclockingLogic.create(1 / MATCHED_SPEED, 4, false);

    private IterativeAssemblyRules() {}

    public static OverclockingLogic profile(boolean matched) { return matched ? MATCHED : NORMAL; }
}
