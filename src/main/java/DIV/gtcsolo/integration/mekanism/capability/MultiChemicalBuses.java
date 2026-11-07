package DIV.gtcsolo.integration.mekanism.capability;

import DIV.gtcsolo.registry.ModMachines;
import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.data.RotationState;
import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Item, fluid and gas buses with independent stores, available from IV through MAX. */
public final class MultiChemicalBuses {

    public static final Map<IO, Map<Integer, MachineDefinition>> LOOKUP = new EnumMap<>(IO.class);
    public static final List<MachineDefinition> ALL = new ArrayList<>();

    private MultiChemicalBuses() {}

    public static void init() {
        for (IO io : new IO[]{IO.IN, IO.OUT}) {
            Map<Integer, MachineDefinition> tiers = new LinkedHashMap<>();
            for (int tier : GTValues.tiersBetween(GTValues.IV, GTValues.MAX)) {
                MachineDefinition definition = register(io, tier);
                tiers.put(tier, definition);
                ALL.add(definition);
            }
            LOOKUP.put(io, tiers);
        }
    }

    private static MachineDefinition register(IO io, int tier) {
        String ioKey = io == IO.IN ? "input" : "output";
        String tierKey = GTValues.VN[tier].toLowerCase(Locale.ROOT);
        String name = "multi_chemical_" + ioKey + "_bus_" + tierKey;

        return ModMachines.REGISTRATE.machine(name,
                        holder -> new MultiChemicalBusMachine(holder, tier, io))
                .langValue(GTValues.VN[tier] + " Multi-Chemical Fluid "
                        + (io == IO.IN ? "Input" : "Output") + " Bus")
                .rotationState(RotationState.ALL)
                .tier(tier)
                .abilities(io == IO.IN ? PartAbility.IMPORT_ITEMS : PartAbility.EXPORT_ITEMS,
                        io == IO.IN ? PartAbility.IMPORT_FLUIDS : PartAbility.EXPORT_FLUIDS,
                        ChemicalPartAbilities.get(io, ChemicalIngredient.Type.GAS))
                .tooltips(
                        Component.translatable("gtcsolo.machine.multi_chemical_bus.desc.items",
                                MultiChemicalBusMachine.itemSlotCount(tier)),
                        Component.translatable("gtcsolo.machine.multi_chemical_bus.desc.fluids",
                                MultiChemicalBusMachine.FLUID_SLOTS,
                                capacityString(MultiChemicalBusMachine.fluidCapacity(tier))),
                        Component.translatable("gtcsolo.machine.multi_chemical_bus.desc.gases",
                                MultiChemicalBusMachine.GAS_SLOTS,
                                capacityString(MultiChemicalBusMachine.gasCapacity(tier))),
                        Component.translatable("gtcsolo.machine.multi_chemical_bus.desc.shared"),
                        Component.translatable("gtcsolo.machine.multi_chemical_bus.desc.retained"),
                        Component.translatable("gtcsolo.machine.multi_chemical_bus.desc." + ioKey))
                .overlayTieredHullRenderer("multi_chemical_bus_" + ioKey)
                .register();
    }

    private static String capacityString(long capacity) {
        return String.format(Locale.ROOT, "%,d", capacity);
    }
}
