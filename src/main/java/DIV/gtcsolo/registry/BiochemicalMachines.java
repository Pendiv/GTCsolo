package DIV.gtcsolo.registry;

import static com.gregtechceu.gtceu.api.pattern.Predicates.*;
import DIV.gtcsolo.machine.BiochemicalProcessMachine;
import DIV.gtcsolo.machine.BiochemicalProcessRules;
import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.data.RotationState;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;
import com.gregtechceu.gtceu.api.pattern.*;
import com.gregtechceu.gtceu.api.recipe.*;
import com.gregtechceu.gtceu.api.registry.GTRegistries;
import com.gregtechceu.gtceu.common.data.*;
import com.gregtechceu.gtceu.common.data.machines.GCYMMachines;
import com.lowdragmc.lowdraglib.gui.texture.ProgressTexture;
import com.lowdragmc.lowdraglib.utils.LocalizationUtils;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import java.util.ArrayList;
import java.util.List;

public final class BiochemicalMachines {
    public static GTRecipeType BIOCHEMICAL_REACTION, MEMBRANE_SEPARATION;
    public static MultiblockMachineDefinition BIOREACTOR, MEMBRANE_SEPARATOR;
    private static final String[] FRONT = {"CCCCC", "CCCCC", "CCSCC", "CCCCC", "CCCCC"};
    private static final String[] SECTION = {"CCCCC", "G###G", "G#P#G", "G###G", "CCCCC"};
    private static final String[] BACK = {"CCCCC", "CCCCC", "CCCCC", "CCCCC", "CCCCC"};
    private BiochemicalMachines() {}

    public static void initRecipeTypes() {
        BIOCHEMICAL_REACTION = registerType("biochemical_reaction");
        MEMBRANE_SEPARATION = registerType("membrane_separation");
    }

    private static GTRecipeType registerType(String name) {
        var id = new ResourceLocation("gtcsolo", name);
        var type = new GTRecipeType(id, "multiblock").setMaxIOSize(6, 6, 6, 6).setEUIO(IO.IN)
                .setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, ProgressTexture.FillDirection.LEFT_TO_RIGHT)
                .addDataInfo(data -> name.equals("membrane_separation")
                        ? LocalizationUtils.format("gtcsolo.biochemical.recipe.area", data.getInt(BiochemicalProcessRules.LOAD) / 16000.0)
                        : LocalizationUtils.format("gtcsolo.biochemical.recipe.load", data.getInt(BiochemicalProcessRules.LOAD)))
                .addDataInfo(data -> LocalizationUtils.format("gtcsolo.biochemical.recipe.residence", data.getInt(BiochemicalProcessRules.MINIMUM_TICKS) / 20.0));
        GTRegistries.register(BuiltInRegistries.RECIPE_TYPE, id, type);
        GTRegistries.register(BuiltInRegistries.RECIPE_SERIALIZER, id, new GTRecipeSerializer());
        GTRegistries.RECIPE_TYPES.register(id, type);
        return type;
    }

    public static void init() {
        BIOREACTOR = register("biochemical_reactor", BIOCHEMICAL_REACTION, "fermenter");
        MEMBRANE_SEPARATOR = register("membrane_separator", MEMBRANE_SEPARATION, "centrifuge");
    }

    private static MultiblockMachineDefinition register(String name, GTRecipeType type, String overlay) {
        return ModMachines.REGISTRATE.multiblock(name, BiochemicalProcessMachine::new)
                .rotationState(RotationState.NON_Y_AXIS).allowExtendedFacing(false).allowFlip(false)
                .recipeType(type).recipeModifiers(true, GTRecipeModifiers.DEFAULT_ENVIRONMENT_REQUIREMENT, BiochemicalProcessMachine::modify)
                .appearanceBlock(GTBlocks.CASING_STAINLESS_CLEAN).pattern(BiochemicalMachines::pattern)
                .shapeInfos(BiochemicalMachines::shapes)
                .tooltips(Component.translatable("gtcsolo.room.tooltip.details"))
                .workableCasingRenderer(new ResourceLocation("gtceu", "block/casings/solid/machine_casing_clean_stainless_steel"),
                        new ResourceLocation("gtceu", "block/machines/" + overlay)).register();
    }

    public static BlockPattern pattern(MultiblockMachineDefinition definition) {
        var casing = blocks(GTBlocks.CASING_STAINLESS_CLEAN.get()).setMinGlobalLimited(30)
                .or(abilities(PartAbility.INPUT_ENERGY).setMinGlobalLimited(1).setMaxGlobalLimited(2).setPreviewCount(2))
                .or(abilities(PartAbility.IMPORT_ITEMS).setMinGlobalLimited(1).setPreviewCount(1))
                .or(abilities(PartAbility.EXPORT_ITEMS).setMinGlobalLimited(1).setPreviewCount(1))
                .or(abilities(PartAbility.IMPORT_FLUIDS).setMinGlobalLimited(1).setPreviewCount(1))
                .or(abilities(PartAbility.EXPORT_FLUIDS).setMinGlobalLimited(1).setPreviewCount(1))
                .or(abilities(PartAbility.PARALLEL_HATCH).setMaxGlobalLimited(1).setPreviewCount(1))
                .or(autoAbilities(true, false, false));
        return FactoryBlockPattern.start(com.gregtechceu.gtceu.api.pattern.util.RelativeDirection.RIGHT,
                        com.gregtechceu.gtceu.api.pattern.util.RelativeDirection.UP,
                        com.gregtechceu.gtceu.api.pattern.util.RelativeDirection.BACK)
                .aisle(FRONT).aisle(SECTION).setRepeatable(1, 4).aisle(BACK)
                .where('S', controller(blocks(definition.getBlock()))).where('C', casing)
                .where('G', blocks(GTBlocks.CASING_LAMINATED_GLASS.get()))
                .where('P', blocks(GTBlocks.CASING_POLYTETRAFLUOROETHYLENE_PIPE.get()))
                .where('#', any()).build();
    }

    public static List<MultiblockShapeInfo> shapes(MultiblockMachineDefinition definition) {
        List<MultiblockShapeInfo> shapes = new ArrayList<>();
        for (int count = 1; count <= 4; count++) {
            var builder = MultiblockShapeInfo.builder().aisle("CCCCC", "CiCoC", "CeSeC", "CwCfC", "CmpCC");
            for (int i = 0; i < count; i++) builder.aisle(SECTION);
            shapes.add(builder.aisle(BACK)
                    .where('C', GTBlocks.CASING_STAINLESS_CLEAN.get()).where('G', GTBlocks.CASING_LAMINATED_GLASS.get())
                    .where('P', GTBlocks.CASING_POLYTETRAFLUOROETHYLENE_PIPE.get()).where('#', Blocks.AIR)
                    .where('S', definition, Direction.NORTH)
                    .where('i', GTMachines.ITEM_IMPORT_BUS[GTValues.IV], Direction.NORTH)
                    .where('o', GTMachines.ITEM_EXPORT_BUS[GTValues.IV], Direction.NORTH)
                    .where('e', GTMachines.ENERGY_INPUT_HATCH[GTValues.IV], Direction.NORTH)
                    .where('w', GTMachines.FLUID_IMPORT_HATCH[GTValues.IV], Direction.NORTH)
                    .where('f', GTMachines.FLUID_EXPORT_HATCH[GTValues.IV], Direction.NORTH)
                    .where('m', GTMachines.MAINTENANCE_HATCH, Direction.NORTH)
                    .where('p', GCYMMachines.PARALLEL_HATCH[GTValues.IV], Direction.NORTH).build());
        }
        return shapes;
    }
}
