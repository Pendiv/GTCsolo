package DIV.gtcsolo.registry;

import DIV.gtcsolo.machine.IterativeAssemblyLineMachine;
import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.data.RotationState;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;
import com.gregtechceu.gtceu.api.pattern.FactoryBlockPattern;
import com.gregtechceu.gtceu.api.pattern.MultiblockShapeInfo;
import com.gregtechceu.gtceu.api.pattern.util.RelativeDirection;
import com.gregtechceu.gtceu.common.data.GCYMBlocks;
import com.gregtechceu.gtceu.common.data.GTBlocks;
import com.gregtechceu.gtceu.common.data.GTMachines;
import com.gregtechceu.gtceu.common.data.GTRecipeModifiers;
import com.gregtechceu.gtceu.common.data.GTRecipeTypes;
import com.gregtechceu.gtceu.common.data.machines.GTResearchMachines;
import com.gregtechceu.gtceu.config.ConfigHolder;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;

import java.util.List;

import static com.gregtechceu.gtceu.api.pattern.Predicates.*;

public final class IterativeAssemblyLines {
    // The exported controller is at the last character, not the last aisle.
    public static final RelativeDirection CHARACTER_DIRECTION = RelativeDirection.FRONT;
    public static final RelativeDirection ROW_DIRECTION = RelativeDirection.UP;
    public static final RelativeDirection AISLE_DIRECTION = RelativeDirection.RIGHT;
    private static final String[][] AISLES = {
            {"AAAAAAAAA", "BBBBBBBBB", "CCCCCCCCC", "AAAAAAAAA"},
            {"AAAAAAAAA", "BDDDDDDDB", "CEEEEEEEC", "AAAAAAAAA"},
            {"AAAAAAAAA", "BDBBBBBDB", "CECCCCCEC", "AAAAAAAAA"},
            {"AAA###AAA", "BDB###BDB", "CEC###CEC", "AAA###AAA"},
            {"AAA###AAF", "BDB###BDB", "CEC###CEG", "AAA###AAH"},
            {"AAA###AAA", "BDB###BDB", "CEC###CEC", "AAA###AAA"},
            {"AAAAAAAAA", "BDBBBBBDB", "CECCCCCEC", "AAAAAAAAA"},
            {"AAAAAAAAA", "BDDDDDDDB", "CEEEEEEEC", "AAAAAAAAA"},
            {"AAAAAAAAA", "BBBBBBBBB", "CCCCCCCCC", "AAAAAAAAA"}
    };

    public static MultiblockMachineDefinition CORE;
    private IterativeAssemblyLines() {}

    public static void init() {
        CORE = ModMachines.REGISTRATE.multiblock("iterative_assembly_line", IterativeAssemblyLineMachine::new)
                .rotationState(RotationState.NON_Y_AXIS)
                .recipeType(GTRecipeTypes.ASSEMBLY_LINE_RECIPES)
                .recipeModifiers(GTRecipeModifiers.DEFAULT_ENVIRONMENT_REQUIREMENT, IterativeAssemblyLineMachine::overclock)
                .alwaysTryModifyRecipe(true)
                .tooltips(Component.translatable("gtcsolo.iterative_assembly.tooltip.summary"),
                        Component.translatable("gtcsolo.iterative_assembly.tooltip.sample"),
                        Component.translatable("gtcsolo.iterative_assembly.tooltip.casing"))
                .appearanceBlock(GCYMBlocks.CASING_LARGE_SCALE_ASSEMBLING)
                .pattern(definition -> {
                    var pattern = FactoryBlockPattern.start(CHARACTER_DIRECTION, ROW_DIRECTION, AISLE_DIRECTION);
                    for (String[] aisle : AISLES) pattern.aisle(aisle);
                    return pattern
                            .where('A', blocks(GCYMBlocks.CASING_LARGE_SCALE_ASSEMBLING.get()).setMinGlobalLimited(115)
                                    .or(autoAbilities(definition.getRecipeTypes()))
                                    .or(abilities(PartAbility.DATA_ACCESS, PartAbility.OPTICAL_DATA_RECEPTION)
                                            .setExactLimit(1).setPreviewCount(1))
                                    .or(abilities(PartAbility.MAINTENANCE).setMaxGlobalLimited(1)))
                            .where('B', blocks(GTBlocks.CASING_LAMINATED_GLASS.get()))
                            .where('C', blocks(GTBlocks.CASING_GRATE.get()))
                            .where('D', blocks(GTBlocks.CASING_ASSEMBLY_LINE.get()))
                            .where('E', blocks(GTBlocks.CASING_ASSEMBLY_CONTROL.get()))
                            .where('F', abilities(PartAbility.IMPORT_ITEMS).setPreviewCount(1))
                            .where('G', controller(blocks(definition.getBlock())))
                            .where('H', abilities(PartAbility.IMPORT_ITEMS).setPreviewCount(1))
                            .where('#', any())
                            .build();
                })
                .shapeInfos(IterativeAssemblyLines::shapes)
                .workableCasingRenderer(new ResourceLocation("gtceu", "block/casings/gcym/large_scale_assembling_casing"),
                        new ResourceLocation("gtceu", "block/multiblock/assembly_line"))
                .register();
    }

    private static List<MultiblockShapeInfo> shapes(MultiblockMachineDefinition definition) {
        var shape = MultiblockShapeInfo.builder();
        var casing = GCYMBlocks.CASING_LARGE_SCALE_ASSEMBLING;
        // The pattern advances east by aisle and north by character when the core faces north.
        for (int column = AISLES[0][0].length() - 1; column >= 0; column--) {
            String[] rows = new String[AISLES[0].length];
            for (int y = 0; y < rows.length; y++) {
                var row = new StringBuilder(AISLES.length);
                for (String[] aisle : AISLES) row.append(aisle[y].charAt(column));
                rows[y] = row.toString();
            }
            if (column == AISLES[0][0].length() - 1) {
                rows[0] = "AMPJFQAAA";
                rows[3] = "AAUUHUUAA";
            }
            shape.aisle(rows);
        }
        shape.where('A', casing)
                .where('B', GTBlocks.CASING_LAMINATED_GLASS)
                .where('C', GTBlocks.CASING_GRATE)
                .where('D', GTBlocks.CASING_ASSEMBLY_LINE)
                .where('E', GTBlocks.CASING_ASSEMBLY_CONTROL)
                .where('F', GTMachines.ITEM_IMPORT_BUS[GTValues.LuV], Direction.NORTH)
                .where('G', definition, Direction.NORTH)
                .where('H', GTMachines.ITEM_IMPORT_BUS[GTValues.ULV], Direction.NORTH)
                .where('J', GTResearchMachines.DATA_ACCESS_HATCH, Direction.NORTH)
                .where('P', GTMachines.ENERGY_INPUT_HATCH[GTValues.LuV], Direction.NORTH)
                .where('Q', GTMachines.ITEM_EXPORT_BUS[GTValues.LV], Direction.NORTH)
                .where('U', GTMachines.FLUID_IMPORT_HATCH[GTValues.IV], Direction.NORTH)
                .where('#', Blocks.AIR);
        if (ConfigHolder.INSTANCE.machines.enableMaintenance) {
            shape.where('M', GTMachines.MAINTENANCE_HATCH, Direction.NORTH);
        } else {
            shape.where('M', casing);
        }
        return List.of(shape.build());
    }
}
