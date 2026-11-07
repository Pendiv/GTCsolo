package DIV.gtcsolo.registry;

import DIV.gtcsolo.block.FusionMk4CasingType;
import DIV.gtcsolo.machine.fusion.FusionMk4Machine;
import DIV.gtcsolo.machine.fusion.FusionMk4Pattern;
import DIV.gtcsolo.render.FusionMk4Renderer;
import com.gregtechceu.gtceu.api.data.RotationState;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;
import com.gregtechceu.gtceu.api.pattern.FactoryBlockPattern;
import com.gregtechceu.gtceu.api.pattern.MultiblockShapeInfo;
import com.gregtechceu.gtceu.common.data.GTBlocks;
import com.gregtechceu.gtceu.common.data.GTMachines;
import com.gregtechceu.gtceu.common.data.GTRecipeModifiers;
import com.gregtechceu.gtceu.common.data.GTRecipeTypes;
import com.gregtechceu.gtceu.common.machine.multiblock.electric.FusionReactorMachine;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.List;

import static com.gregtechceu.gtceu.api.pattern.Predicates.*;

/** Native GT fusion progression extended by one tier, with the supplied MK4 shell. */
public final class FusionMk4Machines {
    public static MultiblockMachineDefinition FUSION_REACTOR_MK4;

    private FusionMk4Machines() {}

    public static void init() {
        int tier = FusionMk4Machine.TIER;
        GTBlocks.ALL_FUSION_CASINGS.put(FusionMk4CasingType.INSTANCE, ModBlocks.FUSION_CASING_MK4);
        FUSION_REACTOR_MK4 = ModMachines.REGISTRATE.multiblock("fusion_reactor_mk4", FusionMk4Machine::new)
                .rotationState(RotationState.ALL)
                .tier(tier)
                .langValue("Fusion Reactor Computer MK IV")
                .recipeType(GTRecipeTypes.FUSION_RECIPES)
                .recipeModifiers(GTRecipeModifiers.DEFAULT_ENVIRONMENT_REQUIREMENT, FusionMk4Machine::recipeModifier)
                .tooltips(
                        Component.translatable("gtceu.machine.fusion_reactor.capacity",
                                FusionMk4Machine.STARTUP_CAPACITY / 1_000_000L),
                        Component.translatable("gtcsolo.machine.fusion_reactor_mk4.no_overclock"),
                        Component.translatable("gtcsolo.machine.fusion_reactor_mk4.description"),
                        Component.translatable("gtcsolo.machine.fusion_reactor_mk4.ports"))
                .appearanceBlock(() -> ModBlocks.FUSION_CASING_MK4.get())
                .pattern(definition -> {
                    var casing = blocks(ModBlocks.FUSION_CASING_MK4.get());
                    var pattern = FactoryBlockPattern.start();
                    for (String[] aisle : FusionMk4Pattern.aisles()) pattern.aisle(aisle);
                    return pattern
                            .where('G', controller(blocks(definition.getBlock())))
                            .where('A', blocks(GTBlocks.FUSION_GLASS.get()).or(casing))
                            .where('B', casing)
                            .where('C', casing.or(abilities(PartAbility.EXPORT_FLUIDS)))
                            .where('D', blocks(GTBlocks.FUSION_COIL.get()))
                            .where('E', blocks(PartAbility.INPUT_ENERGY.getBlockRange(tier, tier)
                                    .toArray(Block[]::new)).setMinGlobalLimited(FusionMk4Machine.ENERGY_PORTS)
                                    .setMaxGlobalLimited(FusionMk4Machine.ENERGY_PORTS)
                                    .setPreviewCount(FusionMk4Machine.ENERGY_PORTS))
                            .where('F', casing.or(abilities(PartAbility.IMPORT_FLUIDS).setMinGlobalLimited(2)))
                            .where('.', any())
                            .where('#', any())
                            .build();
                })
                .shapeInfos(definition -> List.of(preview(definition, false), preview(definition, true)))
                .renderer(() -> new FusionMk4Renderer(FusionMk4CasingType.TEXTURE,
                        new ResourceLocation("gtceu", "block/multiblock/fusion_reactor")))
                .hasTESR(true)
                .register();
        // Extends the native JEI startup-energy label and tier lookup, without replacing MK1-3.
        FusionReactorMachine.registerFusionTier(tier, " (MKIV)");
    }

    private static MultiblockShapeInfo preview(MultiblockMachineDefinition definition, boolean glass) {
        int tier = FusionMk4Machine.TIER;
        var shape = MultiblockShapeInfo.builder();
        for (String[] aisle : FusionMk4Pattern.aisles()) {
            // Same shell; the lower input/output faces point down, upper ones point up.
            aisle[1] = aisle[1].replace('C', 'c').replace('F', 'f');
            shape.aisle(aisle);
        }
        return shape.where('G', definition, Direction.SOUTH)
                .where('A', glass ? GTBlocks.FUSION_GLASS.get() : ModBlocks.FUSION_CASING_MK4.get())
                .where('B', ModBlocks.FUSION_CASING_MK4.get())
                .where('C', GTMachines.FLUID_EXPORT_HATCH[tier], Direction.UP)
                .where('c', GTMachines.FLUID_EXPORT_HATCH[tier], Direction.DOWN)
                .where('D', GTBlocks.FUSION_COIL.get())
                .where('E', GTMachines.ENERGY_INPUT_HATCH[tier], Direction.UP)
                .where('F', GTMachines.FLUID_IMPORT_HATCH[tier], Direction.UP)
                .where('f', GTMachines.FLUID_IMPORT_HATCH[tier], Direction.DOWN)
                .where('.', Blocks.AIR)
                .where('#', Blocks.AIR)
                .build();
    }
}
