package DIV.gtcsolo.registry;

import DIV.gtcsolo.client.renderer.TieredHpcaPartRenderer;
import DIV.gtcsolo.machine.hpca.HpcaTierSpec;
import DIV.gtcsolo.machine.hpca.TieredHpcaActiveCoolerMachine;
import DIV.gtcsolo.machine.hpca.TieredHpcaComputationMachine;
import DIV.gtcsolo.machine.hpca.TieredHpcaEmptyMachine;
import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.data.RotationState;
import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;
import com.gregtechceu.gtceu.api.registry.registrate.MachineBuilder;
import com.gregtechceu.gtceu.common.data.GTMaterials;
import com.gregtechceu.gtceu.common.data.machines.GTResearchMachines;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/** Additional HPCA parts use the native controller's existing component ability and thermal logic. */
public final class TieredHpcaComponents {
    private static final List<MachineDefinition> PARTS = new ArrayList<>();

    private TieredHpcaComponents() {}

    public static void init() {
        if (!PARTS.isEmpty()) return;
        for (var tier : HpcaTierSpec.values()) {
            PARTS.add(builder(tier, "computation", "Computation", holder -> new TieredHpcaComputationMachine(holder, tier))
                    .tooltips(
                            Component.translatable("gtceu.machine.hpca.component_general.upkeep_eut", tier.computationUpkeepEUt()),
                            Component.translatable("gtceu.machine.hpca.component_general.max_eut", tier.computationMaxEUt()),
                            Component.translatable("gtceu.machine.hpca.component_type.computation_cwut", tier.computationPerTick()),
                            Component.translatable("gtceu.machine.hpca.component_type.computation_cooling", tier.computationCoolingDemand()))
                    .tooltipBuilder(GTResearchMachines.OVERHEAT_TOOLTIPS)
                    .renderer(() -> new TieredHpcaPartRenderer(tier, "computation", true))
                    .register());
            PARTS.add(builder(tier, "active_cooler", "Active Cooling", holder -> new TieredHpcaActiveCoolerMachine(holder, tier))
                    .tooltips(
                            Component.translatable("gtceu.machine.hpca.component_general.max_eut", tier.coolerEUt()),
                            Component.translatable("gtceu.machine.hpca.component_type.cooler_active"),
                            Component.translatable("gtceu.machine.hpca.component_type.cooler_active_coolant",
                                    tier.coolantPerTick(), GTMaterials.PCBCoolant.getLocalizedName()),
                            Component.translatable("gtceu.machine.hpca.component_type.cooler_cooling", tier.coolingPerTick()))
                    .renderer(() -> new TieredHpcaPartRenderer(tier, "active_cooler", false))
                    .register());
            PARTS.add(builder(tier, "empty", "Empty", holder -> new TieredHpcaEmptyMachine(holder, tier))
                    .tooltips(Component.translatable("gtceu.machine.hpca.empty_component.tooltip"))
                    .renderer(() -> new TieredHpcaPartRenderer(tier, "empty", false))
                    .register());
        }
    }

    private static MachineBuilder<MachineDefinition> builder(HpcaTierSpec tier, String kind, String displayKind,
                                                             Function<IMachineBlockEntity, MetaMachine> factory) {
        return ModMachines.REGISTRATE.machine(tier.path() + "_hpca_" + kind + "_component", factory)
                .langValue(tier.name() + (kind.equals("empty") ? " Empty HPCA" : " HPCA " + displayKind) + " Component")
                .tier(tier == HpcaTierSpec.UHV ? GTValues.UHV : GTValues.UEV)
                .rotationState(RotationState.ALL)
                .abilities(PartAbility.HPCA_COMPONENT);
    }

    public static List<MachineDefinition> all() {
        return List.copyOf(PARTS);
    }
}
