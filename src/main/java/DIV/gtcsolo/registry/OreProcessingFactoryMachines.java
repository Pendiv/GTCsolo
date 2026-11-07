package DIV.gtcsolo.registry;

import DIV.gtcsolo.machine.orefactory.OreFactoryPattern;
import DIV.gtcsolo.machine.mega.MegaMixerRecipeModifiers;
import com.gregtechceu.gtceu.api.data.RotationState;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.common.data.GCYMBlocks;
import com.gregtechceu.gtceu.common.data.GTRecipeModifiers;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public final class OreProcessingFactoryMachines {
    public static MultiblockMachineDefinition CORE;
    private OreProcessingFactoryMachines() {}
    public static void init() {
        var mods=net.minecraftforge.fml.ModList.get();
        if(!mods.isLoaded("immersiveengineering")||!mods.isLoaded("kubejs"))return;
        CORE=ModMachines.REGISTRATE.multiblock("ore_processing_factory",WorkableElectricMultiblockMachine::new)
                .rotationState(RotationState.NON_Y_AXIS).allowExtendedFacing(false).allowFlip(false)
                .recipeTypes(ModRecipeTypes.ORE_WASHING_PROCESS,ModRecipeTypes.ORE_CHEMICAL_WASHING_PROCESS,ModRecipeTypes.ORE_SEPARATION_PROCESS)
                .recipeModifiers(true,GTRecipeModifiers.DEFAULT_ENVIRONMENT_REQUIREMENT,
                        MegaMixerRecipeModifiers::parallel,MegaMixerRecipeModifiers::subTick)
                .appearanceBlock(GCYMBlocks.CASING_SECURE_MACERATION)
                .pattern(OreFactoryPattern::create).multiblockPreviewRenderer(true,false)
                .tooltips(Component.translatable("gtcsolo.ore_factory.tooltip.routes"),
                        Component.translatable("gtcsolo.ore_factory.tooltip.batch"),
                        Component.translatable("gtcsolo.ore_factory.tooltip.power"))
                .workableCasingRenderer(new ResourceLocation("gtceu","block/casings/gcym/secure_maceration_casing"),
                        new ResourceLocation("gtceu","block/multiblock/gcym/large_maceration_tower"))
                .register();
    }
}
