package DIV.gtcsolo.registry;

import static com.gregtechceu.gtceu.api.pattern.Predicates.*;

import DIV.gtcsolo.api.tier.RoomStructureTier;
import DIV.gtcsolo.api.tier.TieredMultiblockRegistry;
import DIV.gtcsolo.machine.CultivationFormationMachine;
import DIV.gtcsolo.machine.RoomStructures;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.data.RotationState;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;
import com.gregtechceu.gtceu.api.pattern.BlockPattern;
import com.gregtechceu.gtceu.api.pattern.MultiblockShapeInfo;
import com.gregtechceu.gtceu.api.recipe.GTRecipeSerializer;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.api.registry.GTRegistries;
import com.gregtechceu.gtceu.common.data.GTBlocks;
import com.gregtechceu.gtceu.common.data.GTMachines;
import com.gregtechceu.gtceu.common.data.GTRecipeModifiers;
import com.gregtechceu.gtceu.common.data.machines.GCYMMachines;
import com.lowdragmc.lowdraglib.gui.texture.ProgressTexture;

import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class CultivationFormationRooms {
    public static final RoomStructureTier TIERS =
            new RoomStructureTier("gtcsolo_cultivation_formation_tier");
    public static GTRecipeType RECIPE_TYPE;
    public static MultiblockMachineDefinition MACHINE;

    private CultivationFormationRooms() {}

    public static void initRecipeType() {
        var id = new ResourceLocation("gtcsolo", "cultivation_formation");
        RECIPE_TYPE =
                new GTRecipeType(id, "multiblock")
                        .setMaxIOSize(9, 1, 5, 2)
                        .setEUIO(IO.IN)
                        .setProgressBar(
                                GuiTextures.PROGRESS_BAR_ARROW,
                                ProgressTexture.FillDirection.LEFT_TO_RIGHT);
        GTRegistries.register(BuiltInRegistries.RECIPE_TYPE, id, RECIPE_TYPE);
        GTRegistries.register(BuiltInRegistries.RECIPE_SERIALIZER, id, new GTRecipeSerializer());
        GTRegistries.RECIPE_TYPES.register(id, RECIPE_TYPE);
    }

    public static void init() {
        MACHINE =
                ModMachines.REGISTRATE
                        .multiblock(
                                "cultivation_formation_room",
                                holder -> new CultivationFormationMachine(holder, TIERS))
                        .rotationState(RotationState.NON_Y_AXIS)
                        .allowExtendedFacing(false)
                        .allowFlip(false)
                        .recipeType(RECIPE_TYPE)
                        .recipeModifiers(
                                true,
                                GTRecipeModifiers.DEFAULT_ENVIRONMENT_REQUIREMENT,
                                CultivationFormationMachine::modify)
                        .appearanceBlock(GTBlocks.CASING_INVAR_HEATPROOF)
                        .pattern(CultivationFormationRooms::pattern)
                        .shapeInfos(CultivationFormationRooms::shapes)
                        .tooltips(
                                Component.translatable("gtcsolo.cultivation.essences_required"),
                                Component.translatable("gtcsolo.room.tooltip.details"))
                        .workableCasingRenderer(
                                new ResourceLocation(
                                        "gtceu", "block/casings/solid/machine_casing_heatproof"),
                                new ResourceLocation("gtceu", "block/machines/fermenter"))
                        .register();
        TieredMultiblockRegistry.registerPreviewTiers(MACHINE, TIERS.tiers());
    }

    public static BlockPattern pattern(MultiblockMachineDefinition definition) {
        var ports =
                blocks(GTBlocks.CASING_INVAR_HEATPROOF.get())
                        .or(
                                abilities(PartAbility.INPUT_ENERGY)
                                        .setMinGlobalLimited(1)
                                        .setMaxGlobalLimited(2)
                                        .setPreviewCount(1))
                        .or(
                                abilities(PartAbility.IMPORT_ITEMS)
                                        .setMinGlobalLimited(1)
                                        .setPreviewCount(1))
                        .or(
                                abilities(PartAbility.EXPORT_ITEMS)
                                        .setMinGlobalLimited(1)
                                        .setPreviewCount(1))
                        .or(abilities(PartAbility.IMPORT_FLUIDS).setPreviewCount(1))
                        .or(abilities(PartAbility.EXPORT_FLUIDS))
                        .or(
                                abilities(PartAbility.PARALLEL_HATCH)
                                        .setMaxGlobalLimited(1)
                                        .setPreviewCount(1))
                        .or(autoAbilities(true, false, false));
        return RoomStructures.CULTIVATION_FORMATION_ROOM
                .patternBuilder()
                .where('A', ports)
                .where('B', TIERS.casingPredicate())
                .where('C', TIERS.glassPredicate())
                .where('D', controller(blocks(definition.getBlock())))
                .where('#', any())
                .build();
    }

    public static Map<RoomStructures.Cell, Character> previewPorts() {
        return Map.of(
                new RoomStructures.Cell(0, 0, 1),
                'i',
                new RoomStructures.Cell(0, 0, 3),
                'o',
                new RoomStructures.Cell(0, 3, 1),
                'e',
                new RoomStructures.Cell(0, 3, 3),
                'p',
                new RoomStructures.Cell(4, 0, 1),
                'w',
                new RoomStructures.Cell(4, 0, 3),
                'm');
    }

    public static List<MultiblockShapeInfo> shapes(MultiblockMachineDefinition definition) {
        var result = new ArrayList<MultiblockShapeInfo>();
        for (int grade : TIERS.tiers()) {
            int energy = Math.min(grade, GTMachines.ENERGY_INPUT_HATCH.length - 1);
            while (energy > GTValues.LV && GTMachines.ENERGY_INPUT_HATCH[energy] == null) energy--;
            result.add(
                    RoomStructures.CULTIVATION_FORMATION_ROOM
                            .shapeBuilder(previewPorts())
                            .where('A', GTBlocks.CASING_INVAR_HEATPROOF.get())
                            .where('B', TIERS.casingForTier(grade))
                            .where('C', TIERS.previewGlassForTier(grade))
                            .where('#', Blocks.AIR)
                            .where('D', definition, Direction.NORTH)
                            .where('i', GTMachines.ITEM_IMPORT_BUS[GTValues.HV], Direction.NORTH)
                            .where('o', GTMachines.ITEM_EXPORT_BUS[GTValues.MV], Direction.NORTH)
                            .where('e', GTMachines.ENERGY_INPUT_HATCH[energy], Direction.NORTH)
                            .where('p', GCYMMachines.PARALLEL_HATCH[GTValues.IV], Direction.NORTH)
                            .where('w', GTMachines.FLUID_IMPORT_HATCH[GTValues.LV], Direction.SOUTH)
                            .where('m', GTMachines.MAINTENANCE_HATCH, Direction.SOUTH)
                            .build());
        }
        return result;
    }
}
