package DIV.gtcsolo.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import java.util.*;

/** Generated from stone_gen.txt; positions are relative to the normalized, outward-facing controller. */
public final class StoneGeneratorGeometry {
    private static final Map<Character, Map<Direction, Set<BlockPos>>> OFFSETS = new HashMap<>();
    static {
        add('B', new int[][]{{1,2,1},{1,2,2},{1,2,3},{1,2,4},{1,2,5}});
        add('H', new int[][]{{-1,1,1},{-3,2,1},{-2,2,1},{-1,2,1},{-1,1,2},{-3,2,2},{-2,2,2},{-1,2,2},{-1,1,3},{-3,2,3},{-2,2,3},{-1,2,3},{-1,1,4},{-3,2,4},{-2,2,4},{-1,2,4},{-1,1,5},{-3,2,5},{-2,2,5},{-1,2,5}});
        add('G', new int[][]{{0,2,1},{0,2,2},{0,2,3},{0,2,4},{0,2,5}});
    }
    private StoneGeneratorGeometry() {}
    private static void add(char symbol, int[][] cells) {
        var faces = new EnumMap<Direction, Set<BlockPos>>(Direction.class);
        for (var front : List.of(Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST)) {
            var right = front.getClockWise();
            var back = front.getOpposite();
            var positions = new LinkedHashSet<BlockPos>();
            for (var cell : cells) positions.add(new BlockPos(right.getStepX() * cell[0] + back.getStepX() * cell[2],
                    cell[1], right.getStepZ() * cell[0] + back.getStepZ() * cell[2]));
            faces.put(front, Collections.unmodifiableSet(positions));
        }
        OFFSETS.put(symbol, faces);
    }
    public static Set<BlockPos> offsets(char symbol, Direction front) {
        return OFFSETS.getOrDefault(symbol, Map.of()).getOrDefault(front, Set.of());
    }
}
