package DIV.gtcsolo.machine;

import DIV.gtcsolo.registry.ModBlocks;
import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.pattern.MultiblockShapeInfo;
import com.gregtechceu.gtceu.common.data.GTBlocks;
import com.gregtechceu.gtceu.common.data.GTMachines;
import com.gregtechceu.gtceu.config.ConfigHolder;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

import java.util.List;

public final class DampedWashingCleanroomShape {
    private DampedWashingCleanroomShape() {}

    public static List<MultiblockShapeInfo> preview(MultiblockMachineDefinition definition) {
        var door = Blocks.IRON_DOOR.defaultBlockState().setValue(DoorBlock.FACING, Direction.NORTH);
        // Atlas coordinates: north entrance first, then south wall; each aisle runs from floor to ceiling.
        var builder = MultiblockShapeInfo.builder()
                .aisle("XXXXXXXXX", "XEXXOXXMX", "XXGGRGGXX", "XXGGXGGXX", "XXXXXXXXX", "XXXXXXXXX", "XXXXXXXXX")
                .aisle("XXXXXXXXX", "X       X", "X       X", "X       X", "X       X", "X       X", "XFFFFFFFX")
                .aisle("XXXXXXXXX", "X       X", "G       G", "G       G", "X       X", "X       X", "XFFFFFFFX")
                .aisle("XXXXXXXXX", "X       X", "G       G", "G       G", "X       X", "X       X", "XFFFFFFFX")
                .aisle("XXXXXXXXX", "X       X", "G       G", "G       G", "X       X", "X       X", "XFFFSFFFX")
                .aisle("XXXXXXXXX", "X       X", "G       G", "G       G", "X       X", "X       X", "XFFFFFFFX")
                .aisle("XXXXXXXXX", "X       X", "G       G", "G       G", "X       X", "X       X", "XFFFFFFFX")
                .aisle("XXXXXXXXX", "X   A   X", "X       X", "X       X", "X       X", "X       X", "XFFFFFFFX")
                .aisle("XXXXXXXXX", "XXXIDLXXX", "XXXXXXXXX", "XXXXXXXXX", "XXXXXXXXX", "XXXXXXXXX", "XXXXXXXXX")
                .where('X', GTBlocks.PLASTCRETE)
                .where('G', GTBlocks.CLEANROOM_GLASS)
                .where('F', ModBlocks.DAMPED_WASHING_FILTER_CASING)
                .where('S', definition, Direction.NORTH)
                .where('E', GTMachines.ENERGY_INPUT_HATCH[GTValues.LV], Direction.NORTH)
                .where('I', GTMachines.ITEM_PASSTHROUGH_HATCH[GTValues.LV], Direction.SOUTH)
                .where('L', GTMachines.FLUID_PASSTHROUGH_HATCH[GTValues.LV], Direction.SOUTH)
                .where('D', GTMachines.DIODE[GTValues.UV], Direction.NORTH)
                .where('A', GTMachines.CIRCUIT_ASSEMBLER[GTValues.UV], Direction.NORTH)
                .where('O', door.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER))
                .where('R', door.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER))
                .where(' ', Blocks.AIR);
        if (ConfigHolder.INSTANCE.machines.enableMaintenance) {
            builder.where('M', GTMachines.MAINTENANCE_HATCH, Direction.NORTH);
        } else {
            builder.where('M', GTBlocks.PLASTCRETE);
        }
        return List.of(builder.build());
    }
}
