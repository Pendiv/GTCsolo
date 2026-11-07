package DIV.gtcsolo.machine.water;

import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.pattern.BlockPattern;
import com.gregtechceu.gtceu.api.pattern.FactoryBlockPattern;
import com.gregtechceu.gtceu.api.pattern.MultiblockShapeInfo;
import com.gregtechceu.gtceu.api.pattern.Predicates;
import com.gregtechceu.gtceu.api.pattern.util.RelativeDirection;
import com.gregtechceu.gtceu.common.data.GTBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.List;

/** run/cmdex/export/water_server.txt: bottom-to-top rows, front-to-back aisles. */
public final class WaterServerStructure {
    private static final String[][] AISLES = {
            {"ABA", "BBB", "BBB", "CCC"},
            {"BBB", "B#B", "B#B", "CCC"},
            {"ABA", "BBB", "BBB", "CCC"}
    };
    public static final List<BlockPos> CORE_POSITIONS;
    static {
        var positions = new ArrayList<BlockPos>();
        positions.add(new BlockPos(1, 1, 0)); // Default construction/JEI position.
        for (int z = 0; z < 3; z++) for (int y = 0; y < 4; y++) for (int x = 0; x < 3; x++) {
            var position = new BlockPos(x, y, z);
            if (isWoodCell(x, y, z) && !positions.contains(position)) positions.add(position);
        }
        CORE_POSITIONS = List.copyOf(positions);
    }
    private WaterServerStructure() {}

    public static boolean isWoodCell(int x, int y, int z) {
        return x >= 0 && x < 3 && y >= 0 && y < 4 && z >= 0 && z < 3 && AISLES[z][y].charAt(x) == 'B';
    }

    public static boolean containsWall(BlockPos controller, Direction facing, int index, BlockPos wall) {
        if (facing.getAxis().isVertical() || index < 0 || index >= CORE_POSITIONS.size()) return false;
        BlockPos local = CORE_POSITIONS.get(index);
        BlockPos delta = wall.subtract(controller);
        Direction right = facing.getClockWise(), back = facing.getOpposite();
        int x = local.getX() + delta.getX() * right.getStepX() + delta.getZ() * right.getStepZ();
        int z = local.getZ() + delta.getX() * back.getStepX() + delta.getZ() * back.getStepZ();
        return isWoodCell(x, local.getY() + delta.getY(), z);
    }

    public static BlockPattern pattern(MultiblockMachineDefinition definition, int index) {
        return pattern(definition.getBlock(), GTBlocks.CASING_WOOD_WALL.get(), GTBlocks.STEEL_HULL.get(), index);
    }

    /** Material parameters also allow native pattern tests without a running Forge registry. */
    public static BlockPattern pattern(Block core, Block wood, Block steel, int index) {
        BlockPos position = CORE_POSITIONS.get(index);
        var builder = FactoryBlockPattern.start(RelativeDirection.RIGHT, RelativeDirection.UP, RelativeDirection.BACK);
        for (int z = 0; z < AISLES.length; z++) {
            String[] rows = AISLES[z].clone();
            if (z == position.getZ()) {
                int y = position.getY(), x = position.getX();
                rows[y] = rows[y].substring(0, x) + 'S' + rows[y].substring(x + 1);
            }
            builder.aisle(rows);
        }
        return builder.where('S', Predicates.controller(Predicates.blocks(core)))
                .where('A', Predicates.blocks(steel)).where('B', Predicates.blocks(wood))
                .where('C', Predicates.blocks(Blocks.GLASS)).where('#', Predicates.any()).build();
    }

    public static List<MultiblockShapeInfo> previews(MultiblockMachineDefinition definition) {
        var result = new ArrayList<MultiblockShapeInfo>();
        for (int i = 0; i < CORE_POSITIONS.size(); i++)
            result.add(new MultiblockShapeInfo(pattern(definition, i).getPreview(new int[]{1, 1, 1})));
        return result;
    }
}
