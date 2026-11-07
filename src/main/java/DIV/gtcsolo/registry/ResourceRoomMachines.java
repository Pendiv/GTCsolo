package DIV.gtcsolo.registry;

import DIV.gtcsolo.api.tier.RoomStructureTier;
import DIV.gtcsolo.api.tier.TieredMultiblockRegistry;
import DIV.gtcsolo.machine.ResourceRoomMachine;
import DIV.gtcsolo.machine.RoomRecipeModifiers;
import DIV.gtcsolo.machine.RoomStructures;
import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.data.RotationState;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;
import com.gregtechceu.gtceu.api.pattern.BlockPattern;
import com.gregtechceu.gtceu.api.pattern.MultiblockShapeInfo;
import com.gregtechceu.gtceu.api.pattern.TraceabilityPredicate;
import com.gregtechceu.gtceu.common.block.CoilBlock;
import com.gregtechceu.gtceu.common.data.GTBlocks;
import com.gregtechceu.gtceu.common.data.GTMachines;
import com.gregtechceu.gtceu.common.data.GTRecipeModifiers;
import com.gregtechceu.gtceu.common.data.machines.GCYMMachines;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.gregtechceu.gtceu.api.pattern.Predicates.*;

/** The authored room geometry, with tier windows and casing positions kept separate from hatches. */
public final class ResourceRoomMachines {
    public static final RoomStructureTier TIERS = new RoomStructureTier("gtcsolo_resource_room_tier");
    public static MultiblockMachineDefinition FAMING_ROOM;
    public static MultiblockMachineDefinition CONSTRUCTION_ROOM;

    private ResourceRoomMachines() {}

    public static void init() {
        FAMING_ROOM = register(false);
        CONSTRUCTION_ROOM = register(true);
    }

    private static MultiblockMachineDefinition register(boolean construction) {
        var structure = construction ? RoomStructures.CONSTRUCTION_ROOM : RoomStructures.FAMING_ROOM;
        var definition = ModMachines.REGISTRATE.multiblock(structure.id(), holder -> new ResourceRoomMachine(holder, TIERS, construction))
                .rotationState(RotationState.NON_Y_AXIS).allowExtendedFacing(false).allowFlip(false)
                .recipeType(construction ? ModRecipeTypes.CONSTRUCTION_ROOM : ModRecipeTypes.FAMING_ROOM)
                .recipeModifiers(true, GTRecipeModifiers.DEFAULT_ENVIRONMENT_REQUIREMENT, RoomRecipeModifiers::modify)
                .appearanceBlock(GTBlocks.CASING_STEEL_SOLID)
                .pattern(def -> pattern(def, construction))
                .shapeInfos(def -> shapes(def, construction))
                .tooltips(Component.translatable("gtcsolo.room.tooltip.tier"),
                        Component.translatable(construction ? "gtcsolo.room.tooltip.construction" : "gtcsolo.room.tooltip.farming"),
                        Component.translatable("gtcsolo.room.tooltip.details"))
                .workableCasingRenderer(new ResourceLocation("gtceu", "block/casings/solid/machine_casing_solid_steel"),
                        new ResourceLocation("gtceu", construction ? "block/multiblock/electric_blast_furnace" : "block/machines/fermenter"))
                .register();
        TieredMultiblockRegistry.registerPreviewTiers(definition, TIERS.tiers());
        return definition;
    }

    private static TraceabilityPredicate ports() {
        return blocks(GTBlocks.CASING_STEEL_SOLID.get())
                .or(abilities(PartAbility.INPUT_ENERGY).setMinGlobalLimited(1).setMaxGlobalLimited(2).setPreviewCount(1))
                .or(abilities(PartAbility.IMPORT_ITEMS).setMinGlobalLimited(1).setPreviewCount(1))
                .or(abilities(PartAbility.EXPORT_ITEMS).setMinGlobalLimited(1).setPreviewCount(1))
                .or(abilities(PartAbility.IMPORT_FLUIDS).setPreviewCount(1))
                .or(abilities(PartAbility.EXPORT_FLUIDS))
                .or(abilities(PartAbility.PARALLEL_HATCH).setMaxGlobalLimited(1).setPreviewCount(1))
                .or(autoAbilities(true, false, false));
    }

    private static BlockPattern pattern(MultiblockMachineDefinition definition, boolean construction) {
        var structure = construction ? RoomStructures.CONSTRUCTION_ROOM : RoomStructures.FAMING_ROOM;
        var builder = structure.patternBuilder()
                .where(structure.coreSymbol(), controller(blocks(definition.getBlock())))
                .where('A', ports()).where(structure.casingSymbol(), TIERS.casingPredicate())
                .where(structure.glassSymbol(), TIERS.glassPredicate()).where('#', any());
        if (construction) {
            builder.where('B', blocks(GTBlocks.FIREBOX_STEEL.get())).where('E', heatingCoils())
                    .where('G', blocks(GTBlocks.CASING_STEEL_PIPE.get()))
                    .where('H', abilities(PartAbility.MUFFLER).setExactLimit(1));
        } else {
            builder.where('E', blocks(GTBlocks.FIREBOX_STEEL.get()))
                    .where('F', blocks(GTBlocks.STEEL_HULL.get())).where('G', blocks(Blocks.GRASS_BLOCK));
        }
        return builder.build();
    }

    private static List<MultiblockShapeInfo> shapes(MultiblockMachineDefinition definition, boolean construction) {
        var structure = construction ? RoomStructures.CONSTRUCTION_ROOM : RoomStructures.FAMING_ROOM;
        // The substitutions are A positions in both source exports; the core and tier parts are untouched.
        Map<RoomStructures.Cell, Character> ports = previewPorts(construction);
        List<MultiblockShapeInfo> result = new ArrayList<>();
        for (int grade : TIERS.tiers()) {
            var builder = structure.shapeBuilder(ports)
                    .where(structure.coreSymbol(), definition, Direction.NORTH)
                    .where('A', GTBlocks.CASING_STEEL_SOLID.get())
                    .where(structure.casingSymbol(), TIERS.casingForTier(grade))
                    .where(structure.glassSymbol(), TIERS.previewGlassForTier(grade))
                    .where('#', Blocks.AIR)
                    .where('i', GTMachines.ITEM_IMPORT_BUS[GTValues.LV], Direction.NORTH)
                    .where('o', GTMachines.ITEM_EXPORT_BUS[GTValues.LV], Direction.NORTH)
                    .where('e', previewEnergy(grade), Direction.NORTH)
                    .where('p', GCYMMachines.PARALLEL_HATCH[GTValues.IV], Direction.NORTH)
                    .where('w', GTMachines.FLUID_IMPORT_HATCH[GTValues.LV], Direction.SOUTH)
                    .where('m', GTMachines.MAINTENANCE_HATCH, Direction.SOUTH);
            if (construction) {
                builder.where('B', GTBlocks.FIREBOX_STEEL.get())
                        .where('E', GTBlocks.COIL_CUPRONICKEL.get()).where('G', GTBlocks.CASING_STEEL_PIPE.get())
                        .where('H', GTMachines.MUFFLER_HATCH[GTValues.LV], Direction.UP);
            } else {
                builder.where('E', GTBlocks.FIREBOX_STEEL.get()).where('F', GTBlocks.STEEL_HULL.get())
                        .where('G', Blocks.GRASS_BLOCK);
            }
            result.add(builder.build());
        }
        return result;
    }

    public static Map<RoomStructures.Cell, Character> previewPorts(boolean construction) {
        var structure = construction ? RoomStructures.CONSTRUCTION_ROOM : RoomStructures.FAMING_ROOM;
        return Map.of(
                new RoomStructures.Cell(0, 0, 0), 'i', new RoomStructures.Cell(0, 0, structure.width() - 1), 'o',
                new RoomStructures.Cell(0, construction ? 1 : 0, construction ? 0 : 1), 'e',
                new RoomStructures.Cell(0, construction ? 1 : 0, construction ? 4 : structure.width() - 2), 'p',
                new RoomStructures.Cell(4, 0, 0), 'w', new RoomStructures.Cell(4, 0, structure.width() - 1), 'm');
    }

    private static MachineDefinition previewEnergy(int grade) {
        // High-tier hatches can be disabled by GT config; the corresponding room
        // grade still has a valid lower-voltage preview and cannot overclock past it.
        for (int tier = Math.min(grade, GTMachines.ENERGY_INPUT_HATCH.length - 1); tier >= GTValues.LV; tier--) {
            if (GTMachines.ENERGY_INPUT_HATCH[tier] != null) return GTMachines.ENERGY_INPUT_HATCH[tier];
        }
        throw new IllegalStateException("No LV energy hatch registered");
    }
}
