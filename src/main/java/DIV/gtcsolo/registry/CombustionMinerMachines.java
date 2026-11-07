package DIV.gtcsolo.registry;

import DIV.gtcsolo.machine.CombustionMinerMachine;
import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.data.RotationState;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;
import com.gregtechceu.gtceu.api.pattern.BlockPattern;
import com.gregtechceu.gtceu.api.pattern.FactoryBlockPattern;
import com.gregtechceu.gtceu.api.pattern.MultiblockShapeInfo;
import com.gregtechceu.gtceu.api.pattern.TraceabilityPredicate;
import com.gregtechceu.gtceu.api.pattern.util.RelativeDirection;
import com.gregtechceu.gtceu.common.data.GTBlocks;
import com.gregtechceu.gtceu.common.data.GTMachines;
import com.gregtechceu.gtceu.common.data.GTMaterials;
import com.gregtechceu.gtceu.common.data.GTRecipeTypes;
import com.gregtechceu.gtceu.common.data.machines.GTMultiMachines;
import com.lowdragmc.lowdraglib.utils.BlockInfo;
import net.minecraft.core.Direction;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.function.Consumer;

import static com.gregtechceu.gtceu.api.pattern.Predicates.abilities;
import static com.gregtechceu.gtceu.api.pattern.Predicates.any;
import static com.gregtechceu.gtceu.api.pattern.Predicates.blocks;
import static com.gregtechceu.gtceu.api.pattern.Predicates.controller;

public final class CombustionMinerMachines {
    public static final String FUEL_HATCH_POSITION = "gtcsolo.combustion_miner.fuel_hatch";
    public static final String DRILLING_HATCH_POSITION = "gtcsolo.combustion_miner.drilling_hatch";
    private static final ResourceLocation FRAME = new ResourceLocation("gtcsolo", "duplex_2205_frame");

    // CmdExtra miner_iv_new export rotated to face north. Rows run from the base up.
    private static final String[][] AISLES = {
            {"AASAA", "##C##", "##C##", "##C##", "#####", "#####", "#####", "#####", "#####", "#####"},
            {"ABBBA", "#DBD#", "#PBH#", "#ABA#", "#CCC#", "##C##", "##C##", "#####", "#####", "#####"},
            {"HBEBF", "CBEBC", "CBEBC", "CBBBC", "#CBC#", "#CBC#", "#CBC#", "##C##", "##C##", "##C##"},
            {"ABBBA", "#DBD#", "#HBH#", "#ABA#", "#CCC#", "##C##", "##C##", "#####", "#####", "#####"},
            {"AALAA", "##C##", "##C##", "##C##", "#####", "#####", "#####", "#####", "#####", "#####"}
    };

    // CmdExtra miner_hv export rotated so that the controller faces outward.
    private static final String[][] HV_AISLES = {
            {"AASAA", "##C##", "##C##", "##C##", "#####", "#####", "#####", "#####", "#####", "#####"},
            {"ADDDA", "#EDE#", "#DDP#", "#ADA#", "#CCC#", "##C##", "##C##", "#####", "#####", "#####"},
            {"LDHDB", "CDHDC", "CDHDC", "CDDDC", "#CDC#", "#CDC#", "#CDC#", "##C##", "##C##", "##C##"},
            {"ADDDA", "#EDE#", "#DDD#", "#ADA#", "#CCC#", "##C##", "##C##", "#####", "#####", "#####"},
            {"AAFAA", "##C##", "##C##", "##C##", "#####", "#####", "#####", "#####", "#####", "#####"}
    };

    public static MultiblockMachineDefinition HV, IV;

    private CombustionMinerMachines() {}

    public static void init() {
        HV = ModMachines.REGISTRATE.multiblock("hv_combustion_miner", CombustionMinerMachine::new)
                .tier(GTValues.HV)
                .rotationState(RotationState.NON_Y_AXIS).allowExtendedFacing(false).allowFlip(false)
                .recipeType(CombustionMiningRecipes.TYPE).noRecipeModifier()
                .appearanceBlock(GTBlocks.CASING_TUNGSTENSTEEL_ROBUST)
                .pattern(CombustionMinerMachines::hvPattern).shapeInfos(CombustionMinerMachines::hvShapes)
                .workableCasingRenderer(new ResourceLocation("gtceu", "block/casings/solid/machine_casing_robust_tungstensteel"),
                        new ResourceLocation("gtceu", "block/multiblock/large_miner"))
                .register();
        IV = ModMachines.REGISTRATE.multiblock("iv_combustion_miner", CombustionMinerMachine::new)
                .tier(GTValues.IV)
                .rotationState(RotationState.NON_Y_AXIS).allowExtendedFacing(false).allowFlip(false)
                .recipeType(CombustionMiningRecipes.TYPE).noRecipeModifier()
                .appearanceBlock(GTBlocks.CASING_TUNGSTENSTEEL_ROBUST)
                .pattern(CombustionMinerMachines::pattern).shapeInfos(CombustionMinerMachines::shapes)
                .workableCasingRenderer(new ResourceLocation("gtceu", "block/casings/solid/machine_casing_robust_tungstensteel"),
                        new ResourceLocation("gtceu", "block/multiblock/large_miner"))
                .register();
    }

    private static BlockPattern hvPattern(MultiblockMachineDefinition definition) {
        var pattern = FactoryBlockPattern.start(RelativeDirection.RIGHT, RelativeDirection.UP, RelativeDirection.BACK);
        for (String[] aisle : HV_AISLES) pattern.aisle(aisle);
        return pattern
                .where('S', controller(blocks(definition.getBlock())))
                .where('A', blocks(GTBlocks.FIREBOX_TUNGSTENSTEEL.get()))
                .where('B', abilities(PartAbility.EXPORT_ITEMS))
                .where('C', new TraceabilityPredicate(
                        state -> FRAME.equals(ForgeRegistries.BLOCKS.getKey(state.getBlockState().getBlock())),
                        CombustionMinerMachines::frameCandidates))
                .where('D', blocks(GTBlocks.CASING_TUNGSTENSTEEL_ROBUST.get())
                        .or(abilities(PartAbility.IMPORT_ITEMS).setMinGlobalLimited(1)))
                .where('E', blocks(GTBlocks.CASING_EXTREME_ENGINE_INTAKE.get()))
                .where('H', blocks(GTBlocks.CASING_TUNGSTENSTEEL_PIPE.get()))
                .where('P', blocks(GTMachines.ENERGY_INPUT_HATCH[definition.getTier()].getBlock()).setIO(IO.IN)
                        .addTooltips(Component.translatable("gtcsolo.miner.energy_hatch")))
                .where('F', inputHatch(FUEL_HATCH_POSITION, "gtcsolo.miner.fuel_hatch", IO.NONE))
                .where('L', inputHatch(DRILLING_HATCH_POSITION, "gtcsolo.miner.drilling_hatch", IO.IN))
                .where('#', any())
                .build();
    }

    private static List<MultiblockShapeInfo> hvShapes(MultiblockMachineDefinition definition) {
        Block frame = ForgeRegistries.BLOCKS.getValue(FRAME);
        if (frame == null || frame == Blocks.AIR) return List.of();
        var shape = MultiblockShapeInfo.builder();
        for (int index = 0; index < HV_AISLES.length; index++) {
            String[] aisle = HV_AISLES[index];
            if (index == 1) {
                aisle = aisle.clone();
                aisle[2] = "#iDP#";
            }
            shape.aisle(aisle);
        }
        return List.of(shape.where('S', definition, Direction.NORTH)
                .where('A', GTBlocks.FIREBOX_TUNGSTENSTEEL.get())
                .where('B', GTMachines.ITEM_EXPORT_BUS[GTValues.HV], Direction.EAST)
                .where('C', frame)
                .where('D', GTBlocks.CASING_TUNGSTENSTEEL_ROBUST.get())
                .where('E', GTBlocks.CASING_EXTREME_ENGINE_INTAKE.get())
                .where('H', GTBlocks.CASING_TUNGSTENSTEEL_PIPE.get())
                .where('i', GTMachines.ITEM_IMPORT_BUS[GTValues.HV], Direction.WEST)
                .where('P', GTMachines.ENERGY_INPUT_HATCH[definition.getTier()], Direction.EAST)
                .where('F', GTMachines.FLUID_IMPORT_HATCH[GTValues.HV], Direction.SOUTH)
                .where('L', GTMachines.FLUID_IMPORT_HATCH[GTValues.HV], Direction.WEST)
                .where('#', Blocks.AIR)
                .build());
    }

    private static BlockPattern pattern(MultiblockMachineDefinition definition) {
        var pattern = FactoryBlockPattern.start(RelativeDirection.RIGHT, RelativeDirection.UP, RelativeDirection.BACK);
        for (String[] aisle : AISLES) pattern.aisle(aisle);
        return pattern
                .where('S', controller(blocks(definition.getBlock())))
                .where('A', blocks(GTBlocks.FIREBOX_TUNGSTENSTEEL.get()))
                .where('B', blocks(GTBlocks.CASING_TUNGSTENSTEEL_ROBUST.get()))
                .where('C', new TraceabilityPredicate(
                        state -> FRAME.equals(ForgeRegistries.BLOCKS.getKey(state.getBlockState().getBlock())),
                        CombustionMinerMachines::frameCandidates))
                .where('D', blocks(GTBlocks.CASING_EXTREME_ENGINE_INTAKE.get()))
                .where('E', blocks(GTBlocks.CASING_TUNGSTENSTEEL_PIPE.get()))
                .where('P', blocks(GTMachines.ENERGY_INPUT_HATCH[definition.getTier()].getBlock()).setIO(IO.IN)
                        .addTooltips(Component.translatable("gtcsolo.miner.energy_hatch")))
                .where('F', inputHatch(FUEL_HATCH_POSITION, "gtcsolo.miner.fuel_hatch", IO.NONE))
                .where('L', inputHatch(DRILLING_HATCH_POSITION, "gtcsolo.miner.drilling_hatch", IO.IN))
                .where('H', blocks(GTBlocks.CASING_TUNGSTENSTEEL_ROBUST.get())
                        .or(abilities(PartAbility.IMPORT_ITEMS).setMinGlobalLimited(1))
                        .or(abilities(PartAbility.EXPORT_ITEMS).setMinGlobalLimited(1)))
                .where('#', any())
                .build();
    }

    private static TraceabilityPredicate inputHatch(String positionKey, String tooltipKey, IO io) {
        var ability = PartAbility.IMPORT_FLUIDS;
        return new TraceabilityPredicate(state -> {
            if (!ability.isApplicable(state.getBlockState().getBlock())) return false;
            state.getMatchContext().set(positionKey, state.getPos());
            return true;
        }, () -> ability.getAllBlocks().stream().map(BlockInfo::fromBlock).toArray(BlockInfo[]::new))
                .setIO(io).addTooltips(Component.translatable(tooltipKey));
    }

    private static BlockInfo[] frameCandidates() {
        Block frame = ForgeRegistries.BLOCKS.getValue(FRAME);
        return frame == null || frame == Blocks.AIR ? new BlockInfo[0] : new BlockInfo[]{new BlockInfo(frame)};
    }

    private static List<MultiblockShapeInfo> shapes(MultiblockMachineDefinition definition) {
        Block frame = ForgeRegistries.BLOCKS.getValue(FRAME);
        if (frame == null || frame == Blocks.AIR) return List.of();
        var shape = MultiblockShapeInfo.builder();
        for (int index = 0; index < AISLES.length; index++) {
            String[] aisle = AISLES[index];
            if (index == 2) {
                aisle = aisle.clone();
                aisle[0] = "oBEBF";
            }
            if (index == 1) {
                aisle = aisle.clone();
                aisle[2] = "#PBi#";
            }
            shape.aisle(aisle);
        }
        return List.of(shape.where('S', definition, Direction.NORTH)
                .where('A', GTBlocks.FIREBOX_TUNGSTENSTEEL.get())
                .where('B', GTBlocks.CASING_TUNGSTENSTEEL_ROBUST.get())
                .where('C', frame)
                .where('D', GTBlocks.CASING_EXTREME_ENGINE_INTAKE.get())
                .where('E', GTBlocks.CASING_TUNGSTENSTEEL_PIPE.get())
                .where('H', GTBlocks.CASING_TUNGSTENSTEEL_ROBUST.get())
                .where('i', GTMachines.ITEM_IMPORT_BUS[GTValues.IV], Direction.EAST)
                .where('o', GTMachines.ITEM_EXPORT_BUS[GTValues.IV], Direction.WEST)
                .where('P', GTMachines.ENERGY_INPUT_HATCH[definition.getTier()], Direction.WEST)
                .where('F', GTMachines.FLUID_IMPORT_HATCH[GTValues.IV], Direction.EAST)
                .where('L', GTMachines.FLUID_IMPORT_HATCH[GTValues.IV], Direction.SOUTH)
                .where('#', Blocks.AIR)
                .build());
    }

    public static void addControllerRecipe(Consumer<FinishedRecipe> provider) {
        GTRecipeTypes.ASSEMBLER_RECIPES.recipeBuilder(new ResourceLocation("gtcsolo", "hv_combustion_miner"))
                .inputItems(GTMachines.MINER[GTValues.HV])
                .inputItems(GTMachines.COMBUSTION[GTValues.HV], 2)
                .inputItems(GTBlocks.CASING_TUNGSTENSTEEL_ROBUST.asStack(4))
                .inputItems(GTBlocks.CASING_EXTREME_ENGINE_INTAKE.asStack(2))
                .inputItems(GTBlocks.CASING_TUNGSTENSTEEL_PIPE.asStack(2))
                .inputFluids(GTMaterials.SolderingAlloy.getFluid(288))
                .outputItems(HV.asStack())
                .EUt(GTValues.VA[GTValues.HV]).duration(600)
                .save(provider);
        GTRecipeTypes.ASSEMBLER_RECIPES.recipeBuilder(new ResourceLocation("gtcsolo", "iv_combustion_miner"))
                .inputItems(GTMultiMachines.LARGE_MINER[GTValues.IV])
                .inputItems(GTMachines.COMBUSTION[GTValues.HV], 2)
                .inputItems(GTBlocks.CASING_TUNGSTENSTEEL_ROBUST.asStack(4))
                .inputItems(GTBlocks.CASING_EXTREME_ENGINE_INTAKE.asStack(2))
                .inputItems(GTBlocks.CASING_TUNGSTENSTEEL_PIPE.asStack(2))
                .inputFluids(GTMaterials.SolderingAlloy.getFluid(288))
                .outputItems(IV.asStack())
                .EUt(GTValues.VA[GTValues.IV]).duration(600)
                .save(provider);
    }
}
