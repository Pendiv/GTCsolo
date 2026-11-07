package DIV.gtcsolo.registry;

import DIV.gtcsolo.machine.IndustrialCircuitAssemblyLineMachine;
import DIV.gtcsolo.machine.IndustrialCircuitDataHatchMachine;
import DIV.gtcsolo.machine.IndustrialCircuitPattern;
import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.data.RotationState;
import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;
import com.gregtechceu.gtceu.api.pattern.BlockPattern;
import com.gregtechceu.gtceu.common.data.GTBlocks;
import com.gregtechceu.gtceu.common.data.GTRecipeModifiers;
import com.gregtechceu.gtceu.common.data.GTRecipeTypes;
import net.minecraft.network.chat.Component;

import java.util.function.Function;

/** The production core is registered only once a user-approved aisle has been supplied. */
public final class IndustrialCircuitAssemblyLines {
    public static MachineDefinition DATA_HATCH;
    public static MultiblockMachineDefinition CORE;
    private static Function<MultiblockMachineDefinition, BlockPattern> controllerPattern;
    private IndustrialCircuitAssemblyLines() {}

    public static void initSupport() {
        DATA_HATCH = ModMachines.REGISTRATE.machine("industrial_circuit_data_hatch", IndustrialCircuitDataHatchMachine::new)
                .tier(GTValues.LuV).rotationState(RotationState.ALL).abilities(PartAbility.DATA_ACCESS)
                .tooltips(Component.translatable("gtcsolo.circuit_line.data_hatch.desc"))
                .overlayTieredHullRenderer("industrial_circuit_data_hatch").register();
        register(IndustrialCircuitPattern::create);
    }

    public static MultiblockMachineDefinition register(Function<MultiblockMachineDefinition, BlockPattern> aisle) {
        if (aisle == null || CORE != null) throw new IllegalStateException("Supply one approved circuit-line pattern");
        controllerPattern = aisle;
        CORE = ModMachines.REGISTRATE.multiblock("industrial_circuit_assembly_line", IndustrialCircuitAssemblyLineMachine::new)
                .rotationState(RotationState.ALL)
                .recipeTypes(GTRecipeTypes.CIRCUIT_ASSEMBLER_RECIPES, ModRecipeTypes.CIRCUIT_ASSEMBLY_LINE,
                        ModRecipeTypes.CIRCUIT_DATA_ANALYSIS)
                .tooltips(Component.translatable("gtcsolo.ical.tooltip.summary"))
                .tooltips(Component.translatable("gtcsolo.ical.tooltip.detail"))
                .recipeModifiers(GTRecipeModifiers.DEFAULT_ENVIRONMENT_REQUIREMENT)
                .alwaysTryModifyRecipe(true).appearanceBlock(com.gregtechceu.gtceu.common.data.GCYMBlocks.CASING_LARGE_SCALE_ASSEMBLING)
                .pattern(aisle)
                .workableCasingRenderer(new net.minecraft.resources.ResourceLocation("gtceu", "block/casings/gcym/large_scale_assembling_casing"),
                        new net.minecraft.resources.ResourceLocation("gtceu", "block/multiblock/assembly_line"))
                .register();
        return CORE;
    }

    public static BlockPattern newControllerPattern(MultiblockMachineDefinition definition) {
        if (controllerPattern == null) throw new IllegalStateException("Circuit-line aisle is not registered");
        return controllerPattern.apply(definition);
    }
}
