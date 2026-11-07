package DIV.gtcsolo.audit;

import DIV.gtcsolo.machine.RoomRecipeRules;

/** No world or client is started. */
public final class RoomRecipeRulesTest {
    public static void main(String[] args) { run(); }

    public static void run() {
        assert RoomRecipeRules.heatSteps(true, 1799, 900) == 0;
        assert RoomRecipeRules.heatSteps(true, 1800, 900) == 1;
        assert RoomRecipeRules.heatSteps(true, 2700, 900) == 2;
        assert RoomRecipeRules.heatSteps(true, 899, 900) == -1;
        assert RoomRecipeRules.heatSteps(false, 0, 900) == 0;
        assert RoomRecipeRules.maximumStages(32, 31) == -1;
        assert RoomRecipeRules.maximumStages(32, 127) == 0;
        assert RoomRecipeRules.maximumStages(32, 128) == 1;
        assert RoomRecipeRules.maximumStages(32, 512) == 2;
        var farm = RoomRecipeRules.profile(32, 1000, 2, false, 0, 0);
        assert farm.eut() == 512 && farm.duration() == 810;
        assert farm.ioBonus() == 4 && farm.heatSteps() == 0 && farm.heatedStages() == 0;
        var cold = RoomRecipeRules.profile(32, 1000, 2, true, 900, 900);
        assert cold.duration() == 810 && cold.ioBonus() == 16;
        var warm = RoomRecipeRules.profile(32, 1000, 2, true, 1800, 900);
        assert warm.duration() == 765 && warm.ioBonus() == 24 && warm.heatedStages() == 1;
        var hot = RoomRecipeRules.profile(32, 1000, 2, true, 2700, 900);
        assert hot.duration() == 722 && hot.ioBonus() == 32 && hot.heatedStages() == 2;
        var surplus = RoomRecipeRules.profile(32, 1000, 1, true, 4500, 900);
        assert surplus.duration() == 850 && surplus.ioBonus() == 24;
        assert surplus.heatSteps() == 4 && surplus.heatedStages() == 1;
        var noOc = RoomRecipeRules.profile(32, 1000, 0, true, 4500, 900);
        assert noOc.ioBonus() == 12 && noOc.duration() == 1000 && noOc.eut() == 32;
        var sub = RoomRecipeRules.profile(32, 1, 7, false, 0, 0);
        assert sub.subtickParallels() == 2 && sub.duration() == 1 && sub.ioBonus() == 128;
        var hotSub = RoomRecipeRules.profile(32, 1, 5, true, 5400, 900);
        assert hotSub.subtickParallels() == 2 && hotSub.duration() == 1;
        assert RoomRecipeRules.profile(Long.MAX_VALUE, 100, 1, false, 0, 0) == null;
        assert RoomRecipeRules.profile(1, 100, 30, true, Integer.MAX_VALUE, 0) == null;
        assert RoomRecipeRules.profile(1, 0, 1, false, 0, 0) == null;
        assert RoomRecipeRules.profile(32, 100, 2, true, 899, 900) == null;
        System.out.println("Room recipe rules passed: industrial OC, heat thresholds, uncapped thermal IO, sub-tick and overflow.");
    }
}
