package DIV.gtcsolo.audit;

import DIV.gtcsolo.machine.IterativeAssemblyRules;
import com.gregtechceu.gtceu.api.recipe.OverclockingLogic.OCParams;
import DIV.gtcsolo.registry.IterativeAssemblyLines;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/** Exercises the actual GT1.6.4 OC functions used by the new controller. */
final class IterativeAssemblyRegressionTest {
    static void run() {
        for (Direction front : new Direction[]{Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST}) {
            var forward = IterativeAssemblyLines.CHARACTER_DIRECTION.getActualFacing(front);
            var up = IterativeAssemblyLines.ROW_DIRECTION.getActualFacing(front);
            var side = IterativeAssemblyLines.AISLE_DIRECTION.getActualFacing(front);
            assert forward == front : "last character edge must be the controller front";
            assert up == Direction.UP;
            assert side.getAxis() != front.getAxis() && side.getAxis() != Direction.Axis.Y;
            BlockPos core = BlockPos.ZERO;
            BlockPos inside = core.relative(forward, -1);
            assert inside.equals(core.relative(front.getOpposite()));
            assert core.relative(up).equals(core.above()) : "sample stays directly above in all horizontal rotations";
        }
        var params = new OCParams(32, 810, 2, 1);
        var ordinary = IterativeAssemblyRules.profile(false).runOverclockingLogic(params, 512);
        var matched = IterativeAssemblyRules.profile(true).runOverclockingLogic(params, 512);
        assert ordinary.ocLevel() == 2 && matched.ocLevel() == 2;
        assert ordinary.eutMultiplier() == 16 && matched.eutMultiplier() == 16;
        assert Math.abs(810 * ordinary.durationMultiplier() - 250) < 1e-8;
        assert Math.abs(810 * matched.durationMultiplier() - 160) < 1e-8;
        assert ordinary.parallels() == 1 && matched.parallels() == 1;
        var limited = IterativeAssemblyRules.profile(true).runOverclockingLogic(params, 128);
        assert limited.ocLevel() == 1 && limited.eutMultiplier() == 4;
        var singleTick = IterativeAssemblyRules.profile(true)
                .runOverclockingLogic(new OCParams(32, 1, 10, 1), 2097152);
        assert singleTick.ocLevel() == 0 && singleTick.parallels() == 1 : "no unrequested sub-tick/parallel behavior";
        var unchanged = IterativeAssemblyRules.profile(true).runOverclockingLogic(new OCParams(32, 810, 0, 1), 32);
        assert unchanged.eutMultiplier() == 1 && unchanged.durationMultiplier() == 1;
    }
}
