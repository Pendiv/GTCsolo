package DIV.gtcsolo.registry;

import DIV.gtcsolo.machine.IterativeAssemblyLineMachine;
import com.gregtechceu.gtceu.api.data.RotationState;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;
import com.gregtechceu.gtceu.api.pattern.FactoryBlockPattern;
import com.gregtechceu.gtceu.api.pattern.util.RelativeDirection;
import com.gregtechceu.gtceu.common.data.GCYMBlocks;
import com.gregtechceu.gtceu.common.data.GTBlocks;
import com.gregtechceu.gtceu.common.data.GTRecipeModifiers;
import com.gregtechceu.gtceu.common.data.GTRecipeTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import static com.gregtechceu.gtceu.api.pattern.Predicates.*;

public final class IterativeAssemblyLines {
    // The exported controller is at the last character, not the last aisle.
    public static final RelativeDirection CHARACTER_DIRECTION = RelativeDirection.FRONT;
    public static final RelativeDirection ROW_DIRECTION = RelativeDirection.UP;
    public static final RelativeDirection AISLE_DIRECTION = RelativeDirection.RIGHT;
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
                .pattern(definition -> FactoryBlockPattern.start(CHARACTER_DIRECTION, ROW_DIRECTION, AISLE_DIRECTION)
                        .aisle("AAAAAAAAA", "BBBBBBBBB", "CCCCCCCCC", "AAAAAAAAA")
                        .aisle("AAAAAAAAA", "BDDDDDDDB", "CEEEEEEEC", "AAAAAAAAA")
                        .aisle("AAAAAAAAA", "BDBBBBBDB", "CECCCCCEC", "AAAAAAAAA")
                        .aisle("AAA###AAA", "BDB###BDB", "CEC###CEC", "AAA###AAA")
                        .aisle("AAA###AAF", "BDB###BDB", "CEC###CEG", "AAA###AAH")
                        .aisle("AAA###AAA", "BDB###BDB", "CEC###CEC", "AAA###AAA")
                        .aisle("AAAAAAAAA", "BDBBBBBDB", "CECCCCCEC", "AAAAAAAAA")
                        .aisle("AAAAAAAAA", "BDDDDDDDB", "CEEEEEEEC", "AAAAAAAAA")
                        .aisle("AAAAAAAAA", "BBBBBBBBB", "CCCCCCCCC", "AAAAAAAAA")
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
                        .build())
                .workableCasingRenderer(new ResourceLocation("gtceu", "block/casings/gcym/large_scale_assembling_casing"),
                        new ResourceLocation("gtceu", "block/multiblock/assembly_line"))
                .register();
    }
}
