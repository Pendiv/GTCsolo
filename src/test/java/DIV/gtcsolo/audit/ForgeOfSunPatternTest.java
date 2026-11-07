package DIV.gtcsolo.audit;

import DIV.gtcsolo.machine.sun.ForgeOfSunPattern;
import DIV.gtcsolo.registry.ForgeOfSunMachines;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/** Checks all exported cells and the actual native direction mapping without booting a world. */
final class ForgeOfSunPatternTest {
    static void run() {
        var aisles = ForgeOfSunPattern.aisles();
        assert aisles.length == ForgeOfSunPattern.DEPTH;
        Map<Character, Integer> counts = new HashMap<>();
        for (String[] aisle : aisles) {
            assert aisle.length == ForgeOfSunPattern.HEIGHT;
            for (String row : aisle) {
                assert row.length() == ForgeOfSunPattern.WIDTH;
                for (char cell : row.toCharArray()) counts.merge(cell, 1, Integer::sum);
            }
        }
        assert counts.equals(Map.ofEntries(Map.entry('A', 1541), Map.entry('#', 6765),
                Map.entry('B', 160), Map.entry('C', 939), Map.entry('D', 448), Map.entry('E', 448),
                Map.entry('F', 229), Map.entry('G', 435), Map.entry('H', 156), Map.entry('I', 182),
                Map.entry('J', 144), Map.entry('K', 464), Map.entry('L', 1), Map.entry('M', 1)));
        assert counts.get('A') == ForgeOfSunPattern.EXTREME_CASING_COUNT;
        assert aisles[ForgeOfSunPattern.CONTROLLER_AISLE][ForgeOfSunPattern.CONTROLLER_ROW]
                .charAt(ForgeOfSunPattern.CONTROLLER_COLUMN) == 'L';
        assert aisles[9][32].charAt(9) == 'M';
        for (Direction front : new Direction[]{Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST}) {
            var forward = ForgeOfSunMachines.CHARACTER_DIRECTION.getActualFacing(front);
            var up = ForgeOfSunMachines.ROW_DIRECTION.getActualFacing(front);
            var side = ForgeOfSunMachines.AISLE_DIRECTION.getActualFacing(front);
            assert forward == front.getOpposite() && up == Direction.UP && side == front.getCounterClockWise();
            var positions = new HashSet<BlockPos>();
            for (int a = 0; a < aisles.length; a++) for (int y = 0; y < aisles[a].length; y++)
                for (int x = 0; x < aisles[a][y].length(); x++) {
                    var pos = BlockPos.ZERO.relative(forward, x - ForgeOfSunPattern.CONTROLLER_COLUMN)
                            .relative(up, y - ForgeOfSunPattern.CONTROLLER_ROW)
                            .relative(side, a - ForgeOfSunPattern.CONTROLLER_AISLE);
                    assert positions.add(pos) : "rotated cells must not overlap";
                }
            assert positions.size() == 11913;
            assert !positions.contains(BlockPos.ZERO.relative(front)) : "controller must face outside";
        }
        aisles[0][0] = "mutated";
        assert ForgeOfSunPattern.aisles()[0][0].length() == 19 : "preview must not mutate shared pattern";
    }
}
