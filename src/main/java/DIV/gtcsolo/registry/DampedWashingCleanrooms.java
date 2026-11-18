package DIV.gtcsolo.registry;

import DIV.gtcsolo.machine.DampedWashingCleanroomMachine;
import DIV.gtcsolo.machine.DampedWashingCleanroomShape;
import com.gregtechceu.gtceu.api.data.RotationState;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;
import com.gregtechceu.gtceu.api.pattern.BlockPattern;
import com.gregtechceu.gtceu.api.pattern.FactoryBlockPattern;
import com.gregtechceu.gtceu.common.data.GTBlocks;
import com.gregtechceu.gtceu.common.data.GTMachines;
import com.gregtechceu.gtceu.common.data.GTRecipeTypes;
import com.gregtechceu.gtceu.config.ConfigHolder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import static com.gregtechceu.gtceu.api.pattern.Predicates.*;

public final class DampedWashingCleanrooms {
    public static MultiblockMachineDefinition CORE;

    private DampedWashingCleanrooms() {}

    public static void init() {
        CORE = ModMachines.REGISTRATE.multiblock("damped_washing_cleanroom", DampedWashingCleanroomMachine::new)
                .rotationState(RotationState.NONE)
                .allowExtendedFacing(false)
                .allowFlip(false)
                .recipeType(GTRecipeTypes.DUMMY_RECIPES)
                .appearanceBlock(GTBlocks.PLASTCRETE)
                .pattern(DampedWashingCleanrooms::minimumPattern)
                .shapeInfos(DampedWashingCleanroomShape::preview)
                .tooltips(Component.translatable("gtcsolo.machine.damped_washing_cleanroom.dimensions"))
                .workableCasingRenderer(new ResourceLocation("gtceu", "block/casings/cleanroom/plascrete"),
                        new ResourceLocation("gtceu", "block/multiblock/cleanroom"))
                .register();
    }

    private static BlockPattern minimumPattern(MultiblockMachineDefinition definition) {
        var wall = blocks(GTBlocks.PLASTCRETE.get(), GTBlocks.CLEANROOM_GLASS.get())
                .or(abilities(PartAbility.INPUT_ENERGY).setMinGlobalLimited(1).setMaxGlobalLimited(2))
                .or(blocks(GTMachines.MAINTENANCE_HATCH.getBlock(), GTMachines.AUTO_MAINTENANCE_HATCH.getBlock())
                        .setMinGlobalLimited(ConfigHolder.INSTANCE.machines.enableMaintenance ? 1 : 0).setMaxGlobalLimited(1))
                .or(abilities(PartAbility.PASSTHROUGH_HATCH).setMaxGlobalLimited(30));
        return FactoryBlockPattern.start()
                .aisle("XXXXX", "XXXXX", "XXXXX", "XXXXX", "DDDDD")
                .aisle("XXXXX", "X   X", "X   X", "X   X", "DFFFD")
                .aisle("XXXXX", "X   X", "X   X", "X   X", "DFCFD")
                .aisle("XXXXX", "X   X", "X   X", "X   X", "DFFFD")
                .aisle("XXXXX", "XXXXX", "XXXXX", "XXXXX", "DDDDD")
                .where('X', wall)
                .where('D', blocks(GTBlocks.PLASTCRETE.get()))
                .where('F', blocks(ModBlocks.DAMPED_WASHING_FILTER_CASING.get()))
                .where('C', controller(blocks(definition.getBlock())))
                .where(' ', any())
                .build();
    }
}
