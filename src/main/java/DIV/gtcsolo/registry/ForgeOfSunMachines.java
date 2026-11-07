package DIV.gtcsolo.registry;

import DIV.gtcsolo.machine.sun.ForgeOfSunPattern;
import com.gregtechceu.gtceu.api.data.RotationState;
import com.gregtechceu.gtceu.api.data.chemical.ChemicalHelper;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;
import com.gregtechceu.gtceu.api.pattern.FactoryBlockPattern;
import com.gregtechceu.gtceu.api.pattern.util.RelativeDirection;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.common.data.GCYMBlocks;
import com.gregtechceu.gtceu.common.data.GTBlocks;
import com.gregtechceu.gtceu.common.data.GTMaterials;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import static com.gregtechceu.gtceu.api.pattern.Predicates.*;

/** Formation/preview only. No recipe logic or working behavior until the process is designed. */
public final class ForgeOfSunMachines {
    public static final RelativeDirection CHARACTER_DIRECTION = RelativeDirection.BACK;
    public static final RelativeDirection ROW_DIRECTION = RelativeDirection.UP;
    public static final RelativeDirection AISLE_DIRECTION = RelativeDirection.LEFT;
    public static MultiblockMachineDefinition CORE;
    private ForgeOfSunMachines() {}

    public static void init() {
        CORE = ModMachines.REGISTRATE.multiblock("forge_of_sun", MultiblockControllerMachine::new)
                .rotationState(RotationState.NON_Y_AXIS)
                .appearanceBlock(ModBlocks.DRACONIC_STEEL_CASING)
                .tooltips(Component.translatable("gtcsolo.forge_of_sun.tooltip.purpose"),
                        Component.translatable("gtcsolo.forge_of_sun.tooltip.pending"))
                .pattern(definition -> {
                    // L moved to column zero. Reverse both horizontal axes to keep the shape's handedness.
                    var pattern = FactoryBlockPattern.start(CHARACTER_DIRECTION, ROW_DIRECTION, AISLE_DIRECTION);
                    for (String[] aisle : ForgeOfSunPattern.aisles()) pattern.aisle(aisle);
                    return pattern
                            .where('A', blocks(ModBlocks.DRACONIC_STEEL_CASING.get()))
                            .where('B', blocks(ChemicalHelper.getBlock(TagPrefix.frameGt, GTMaterials.Iridium)))
                            .where('C', blocks(GTBlocks.SUPERCONDUCTING_COIL.get()))
                            .where('D', heatingCoils())
                            .where('E', blocks(ModBlocks.MITHRILITE_PIPE_CASING.get()))
                            .where('F', blocks(ModBlocks.MITHRILITE_ROBUST_MACHINE_CASING.get()))
                            .where('G', blocks(ModBlocks.MITHRILITE_FIREBOX_CASING.get()))
                            .where('H', blocks(ModBlocks.MITHRILITE_EXTREME_ENGINE_INTAKE_CASING.get()))
                            .where('I', blocks(GCYMBlocks.CASING_ATOMIC.get()))
                            .where('J', blocks(GTBlocks.FUSION_COIL.get()))
                            .where('K', blocks(ModBlocks.FUSION_CASING_MK4.get()))
                            .where('L', controller(blocks(definition.getBlock())))
                            .where('M', abilities(PartAbility.MUFFLER).setExactLimit(1).setPreviewCount(1))
                            .where('#', any())
                            .build();
                })
                .workableCasingRenderer(new ResourceLocation("gtcsolo",
                                "block/casings/solid/machine_casing_extreme_temperature_draconic_steel"),
                        new ResourceLocation("gtceu", "block/multiblock/electric_blast_furnace"))
                .register();
        CORE.setRecipeTypes(new GTRecipeType[0]);
    }
}
