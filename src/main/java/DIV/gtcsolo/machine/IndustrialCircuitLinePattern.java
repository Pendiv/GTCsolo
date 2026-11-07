package DIV.gtcsolo.machine;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.pattern.Predicates;
import com.gregtechceu.gtceu.api.pattern.TraceabilityPredicate;
import com.gregtechceu.gtceu.common.data.GTMachines;
import com.gregtechceu.gtceu.common.machine.multiblock.part.ItemBusPartMachine;
import net.minecraft.core.BlockPos;

import java.util.LinkedHashMap;
import java.util.Map;

/** Input roles in the authored ICAL aisle; counters belong to the individual match context. */
public final class IndustrialCircuitLinePattern {
    public static final String BASE = "gtcsolo_circuit_base_inputs";
    public static final String BONUS = "gtcsolo_circuit_bonus_inputs";
    public static final String BONUS_FLUID = "gtcsolo_circuit_bonus_fluids";
    private IndustrialCircuitLinePattern() {}

    /** Baseline materials use one-slot ULV buses, as on the native assembly line. */
    public static TraceabilityPredicate baseInput() {
        return Predicates.custom(state -> {
            if (!state.getBlockState().is(GTMachines.ITEM_IMPORT_BUS[GTValues.ULV].getBlock())) return false;
            var machine = state.getTileEntity() instanceof com.gregtechceu.gtceu.api.machine.IMachineBlockEntity holder
                    ? holder.getMetaMachine() : null;
            if (!(machine instanceof ItemBusPartMachine bus) || bus.getTier() != GTValues.ULV
                    || bus.getInventory().getHandlerIO() != com.gregtechceu.gtceu.api.capability.recipe.IO.IN) return false;
            if (!Predicates.abilities(com.gregtechceu.gtceu.api.machine.multiblock.PartAbility.IMPORT_ITEMS).test(state)) return false;
            roles(state.getMatchContext().getOrCreate(BASE, LinkedHashMap::new)).put(state.getPos().immutable(), state.predicate);
            return true;
        }, () -> new com.lowdragmc.lowdraglib.utils.BlockInfo[]{
                com.lowdragmc.lowdraglib.utils.BlockInfo.fromBlockState(GTMachines.ITEM_IMPORT_BUS[GTValues.ULV].getBlock().defaultBlockState())});
    }

    public static TraceabilityPredicate bonusFluidInput() {
        return Predicates.custom(state -> {
            if (!Predicates.abilities(com.gregtechceu.gtceu.api.machine.multiblock.PartAbility.IMPORT_FLUIDS).test(state)) return false;
            roles(state.getMatchContext().getOrCreate(BONUS_FLUID, LinkedHashMap::new)).put(state.getPos().immutable(), state.predicate);
            return true;
        }, () -> com.gregtechceu.gtceu.api.machine.multiblock.PartAbility.IMPORT_FLUIDS.getAllBlocks().stream()
                .map(block -> com.lowdragmc.lowdraglib.utils.BlockInfo.fromBlockState(block.defaultBlockState()))
                .toArray(com.lowdragmc.lowdraglib.utils.BlockInfo[]::new));
    }

    /** One full material set per marked upper bus grants one extra output copy. */
    public static TraceabilityPredicate bonusInput() {
        return Predicates.custom(state -> {
            var machine = state.getTileEntity() instanceof com.gregtechceu.gtceu.api.machine.IMachineBlockEntity holder
                    ? holder.getMetaMachine() : null;
            if (!(machine instanceof ItemBusPartMachine bus)
                    || bus.getInventory().getHandlerIO() != com.gregtechceu.gtceu.api.capability.recipe.IO.IN) return false;
            if (!Predicates.abilities(com.gregtechceu.gtceu.api.machine.multiblock.PartAbility.IMPORT_ITEMS).test(state)) return false;
            roles(state.getMatchContext().getOrCreate(BONUS, LinkedHashMap::new)).put(state.getPos().immutable(), state.predicate);
            return true;
        }, () -> com.gregtechceu.gtceu.api.machine.multiblock.PartAbility.IMPORT_ITEMS.getAllBlocks().stream()
                .map(block -> com.lowdragmc.lowdraglib.utils.BlockInfo.fromBlockState(block.defaultBlockState()))
                .toArray(com.lowdragmc.lowdraglib.utils.BlockInfo[]::new));
    }

    @SuppressWarnings("unchecked")
    private static Map<BlockPos, TraceabilityPredicate> roles(Object value) { return (Map<BlockPos, TraceabilityPredicate>) value; }
}
