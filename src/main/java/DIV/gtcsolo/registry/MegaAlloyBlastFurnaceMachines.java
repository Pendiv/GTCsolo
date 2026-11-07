package DIV.gtcsolo.registry;

import DIV.gtcsolo.machine.mega.*;
import com.gregtechceu.gtceu.api.data.RotationState;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.common.data.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public final class MegaAlloyBlastFurnaceMachines {
    public static MultiblockMachineDefinition CORE;
    private MegaAlloyBlastFurnaceMachines() {}
    public static void init() {
        if (!net.minecraftforge.fml.ModList.get().isLoaded("immersiveengineering")) return;
        CORE = ModMachines.REGISTRATE.multiblock("mega_alloy_blast_furnace", MegaAlloyBlastFurnaceMachine::new)
                .rotationState(RotationState.NON_Y_AXIS).allowExtendedFacing(false).allowFlip(false)
                .recipeTypes(GCYMRecipeTypes.ALLOY_BLAST_RECIPES, GTRecipeTypes.BLAST_RECIPES, ModRecipeTypes.EXTREME_TEMPERATURE_BLAST_FURNACE)
                .recipeModifiers(true, GTRecipeModifiers.DEFAULT_ENVIRONMENT_REQUIREMENT,
                        MegaAlloyBlastFurnaceMachine::batch, MegaMixerRecipeModifiers::parallel,
                        MegaAlloyBlastFurnaceMachine::temperatureOC)
                .appearanceBlock(GCYMBlocks.CASING_HIGH_TEMPERATURE_SMELTING)
                .pattern(MegaBlastPattern::create).multiblockPreviewRenderer(true, false)
                .tooltips(Component.translatable("gtcsolo.mega_blast.tooltip.heat"),
                        Component.translatable("gtcsolo.mega_blast.tooltip.batch"),
                        Component.translatable("gtcsolo.mega_blast.tooltip.maximum"),
                        Component.translatable("gtcsolo.mega_blast.tooltip.cooling"),
                        Component.translatable("gtcsolo.mega_blast.tooltip.structure"))
                .workableCasingRenderer(new ResourceLocation("gtceu", "block/casings/gcym/high_temperature_smelting_casing"),
                        new ResourceLocation("gtceu", "block/multiblock/gcym/blast_alloy_smelter"))
                .register();
    }
}
