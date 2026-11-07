package DIV.gtcsolo.registry;

import DIV.gtcsolo.machine.mega.MegaMixerPattern;
import DIV.gtcsolo.machine.mega.MegaMixerMachine;
import DIV.gtcsolo.machine.mega.MegaMixerRecipeModifiers;
import com.gregtechceu.gtceu.api.data.RotationState;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.common.data.GCYMBlocks;
import com.gregtechceu.gtceu.common.data.GTRecipeModifiers;
import com.gregtechceu.gtceu.common.data.GTRecipeTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public final class MegaMixerMachines {
    public static MultiblockMachineDefinition CORE;
    private MegaMixerMachines() {}

    public static void init() {
        // The real scaffold/stair/fence blocks belong to IE; do not break other
        // installations' JEI registration by advertising an unavailable pattern.
        if (!net.minecraftforge.fml.ModList.get().isLoaded("immersiveengineering")) return;
        CORE = ModMachines.REGISTRATE.multiblock("mega_mixer", MegaMixerMachine::new)
                .rotationState(RotationState.NON_Y_AXIS).allowExtendedFacing(false).allowFlip(false)
                .recipeTypes(GTRecipeTypes.MIXER_RECIPES, ModRecipeTypes.MULTI_MIXING)
                .recipeModifiers(true, GTRecipeModifiers.DEFAULT_ENVIRONMENT_REQUIREMENT,
                        MegaMixerRecipeModifiers::batch, MegaMixerRecipeModifiers::parallel,
                        MegaMixerRecipeModifiers::subTick)
                .appearanceBlock(GCYMBlocks.CASING_REACTION_SAFE)
                .pattern(MegaMixerPattern::create)
                .multiblockPreviewRenderer(true, false)
                .tooltips(Component.translatable("gtcsolo.mega_mixer.tooltip.batch"),
                        Component.translatable("gtcsolo.mega_mixer.tooltip.parallel"),
                        Component.translatable("gtcsolo.mega_mixer.tooltip.power"),
                        Component.translatable("gtcsolo.mega_mixer.tooltip.structure"))
                .workableCasingRenderer(new ResourceLocation("gtceu", "block/casings/gcym/reaction_safe_mixing_casing"),
                        new ResourceLocation("gtceu", "block/multiblock/gcym/large_mixer"))
                .register();
    }
}
