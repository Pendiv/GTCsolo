package DIV.gtcsolo.registry;

import DIV.gtcsolo.api.tier.RoomStructureTier;
import DIV.gtcsolo.api.tier.TieredMultiblockRegistry;
import DIV.gtcsolo.machine.IrradiationSeparationMachine;
import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.data.RotationState;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;
import com.gregtechceu.gtceu.api.pattern.BlockPattern;
import com.gregtechceu.gtceu.api.pattern.FactoryBlockPattern;
import com.gregtechceu.gtceu.api.pattern.MultiblockShapeInfo;
import com.gregtechceu.gtceu.api.pattern.TraceabilityPredicate;
import com.gregtechceu.gtceu.api.pattern.util.RelativeDirection;
import com.gregtechceu.gtceu.common.data.GCYMBlocks;
import com.gregtechceu.gtceu.common.data.GTMachines;
import com.gregtechceu.gtceu.common.data.GTRecipeModifiers;
import com.lowdragmc.lowdraglib.utils.BlockInfo;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

import static com.gregtechceu.gtceu.api.pattern.Predicates.abilities;
import static com.gregtechceu.gtceu.api.pattern.Predicates.any;
import static com.gregtechceu.gtceu.api.pattern.Predicates.autoAbilities;
import static com.gregtechceu.gtceu.api.pattern.Predicates.blocks;
import static com.gregtechceu.gtceu.api.pattern.Predicates.controller;

public final class IrradiationSeparationChambers {
    private static final ResourceLocation CASING = new ResourceLocation("gtcsolo", "sus310s_heatproof_casing");
    private static final ResourceLocation HASTELLOY_PIPE = new ResourceLocation("gtcsolo", "hastelloy_c276_pipe_casing");
    private static final ResourceLocation STAINLESS_PIPE = new ResourceLocation("gtcsolo", "sus316l_pipe_casing");
    private static final RoomStructureTier GLASS = new RoomStructureTier("gtcsolo_irradiation_glass_tier");
    private static final List<Integer> PREVIEW_TIERS = GLASS.tiers().stream().filter(grade -> grade >= GTValues.IV).toList();
    // CmdExtra Irradiation_separation_chamber export, with the controller facing out of its end wall.
    private static final String[][] AISLES = {
            {"AABAA", "ACCCA", "ACCCA", "ACCCA", "AAAAA"},
            {"AAAAA", "ADEDA", "AEEEA", "ADEDA", "AAFAA"},
            {"AAAAA", "AEFEA", "AE#EA", "AEFEA", "AFFFA"},
            {"AAAAA", "AEEEA", "AE#EA", "AEEEA", "AAFAA"},
            {"AAAAA", "AEEEA", "AEEEA", "AEEEA", "AAAAA"},
            {"AAAAA", "AAAAA", "AAAAA", "AAAAA", "AAAAA"}
    };

    private IrradiationSeparationChambers() {}

    public static void init() {
        var definition = ModMachines.REGISTRATE.multiblock("irradiation_separation_chamber",
                        holder -> new IrradiationSeparationMachine(holder, GLASS))
                .tier(GTValues.IV)
                .rotationState(RotationState.NON_Y_AXIS).allowExtendedFacing(false).allowFlip(false)
                .recipeType(ModRecipeTypes.IRRADIATION_SEPARATION)
                .recipeModifiers(true, GTRecipeModifiers.DEFAULT_ENVIRONMENT_REQUIREMENT, GTRecipeModifiers.OC_NON_PERFECT)
                .appearanceBlock(() -> {
                    var casing = ForgeRegistries.BLOCKS.getValue(CASING);
                    return casing == null || casing == Blocks.AIR ? GCYMBlocks.CASING_LASER_SAFE_ENGRAVING.get() : casing;
                })
                .pattern(IrradiationSeparationChambers::pattern)
                .shapeInfos(IrradiationSeparationChambers::shapes)
                .tooltips(Component.translatable("gtcsolo.irradiation_separation"),
                        Component.translatable("gtcsolo.room.tooltip.details"))
                .workableCasingRenderer(new ResourceLocation("gtcsolo", "block/casings/industrial/sus310s_heatproof_casing"),
                        new ResourceLocation("gtceu", "block/multiblock/gcym/large_engraving_laser"))
                .register();
        TieredMultiblockRegistry.registerPreviewTiers(definition, PREVIEW_TIERS);
    }

    private static BlockPattern pattern(MultiblockMachineDefinition definition) {
        var shell = registeredBlock(CASING)
                .or(abilities(PartAbility.IMPORT_ITEMS).setMinGlobalLimited(1).setPreviewCount(1))
                .or(autoAbilities(definition.getRecipeTypes(), true, false, false, true, true, true))
                .or(autoAbilities(true, false, false));
        var builder = FactoryBlockPattern.start(RelativeDirection.RIGHT, RelativeDirection.UP, RelativeDirection.BACK);
        for (String[] aisle : AISLES) builder.aisle(aisle);
        return builder.where('A', shell)
                .where('B', controller(blocks(definition.getBlock())))
                .where('C', GLASS.glassPredicate(GTValues.IV, "gtcsolo.multiblock.error.irradiation_glass_tier"))
                .where('D', registeredBlock(HASTELLOY_PIPE))
                .where('E', blocks(GCYMBlocks.CASING_LASER_SAFE_ENGRAVING.get()))
                .where('F', registeredBlock(STAINLESS_PIPE))
                .where('#', any())
                .build();
    }

    private static TraceabilityPredicate registeredBlock(ResourceLocation id) {
        return new TraceabilityPredicate(state -> id.equals(ForgeRegistries.BLOCKS.getKey(state.getBlockState().getBlock())),
                () -> {
                    var block = ForgeRegistries.BLOCKS.getValue(id);
                    return block == null || block == Blocks.AIR ? new BlockInfo[0] : new BlockInfo[]{BlockInfo.fromBlock(block)};
                });
    }

    private static List<MultiblockShapeInfo> shapes(MultiblockMachineDefinition definition) {
        var casing = ForgeRegistries.BLOCKS.getValue(CASING);
        var hastelloy = ForgeRegistries.BLOCKS.getValue(HASTELLOY_PIPE);
        var stainless = ForgeRegistries.BLOCKS.getValue(STAINLESS_PIPE);
        if (casing == null || hastelloy == null || stainless == null ||
                casing == Blocks.AIR || hastelloy == Blocks.AIR || stainless == Blocks.AIR) return List.of();

        return PREVIEW_TIERS.stream().map(grade -> {
            var shape = MultiblockShapeInfo.builder().aisle("iABAe", "oCCCw", "fCCCm", "ACCCA", "AAAAA");
            for (int aisle = 1; aisle < AISLES.length; aisle++) shape.aisle(AISLES[aisle]);
            return shape.where('A', casing)
                    .where('B', definition, Direction.NORTH)
                    .where('C', GLASS.previewGlassForTier(grade))
                    .where('D', hastelloy)
                    .where('E', GCYMBlocks.CASING_LASER_SAFE_ENGRAVING.get())
                    .where('F', stainless)
                    .where('#', Blocks.AIR)
                    .where('e', ResourceRoomMachines.previewEnergy(grade), Direction.NORTH)
                    .where('i', GTMachines.ITEM_IMPORT_BUS[GTValues.IV], Direction.NORTH)
                    .where('o', GTMachines.ITEM_EXPORT_BUS[GTValues.IV], Direction.NORTH)
                    .where('w', GTMachines.FLUID_IMPORT_HATCH[GTValues.IV], Direction.NORTH)
                    .where('f', GTMachines.FLUID_EXPORT_HATCH[GTValues.IV], Direction.NORTH)
                    .where('m', GTMachines.MAINTENANCE_HATCH, Direction.NORTH)
                    .build();
        }).toList();
    }
}
