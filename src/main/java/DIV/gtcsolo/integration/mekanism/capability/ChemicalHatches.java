package DIV.gtcsolo.integration.mekanism.capability;

import DIV.gtcsolo.registry.ModMachines;
import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.data.RotationState;
import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;
import com.mojang.logging.LogUtils;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Mekanism chemical IO hatch 登録. 3 variant × IO 2 種 × tier 9 段階 = 54 machine.
 *
 * variant:
 *  - GAS      : gas 単一 (16k mb base)
 *  - INFUSION : infusion 単一 (4k mb base)
 *  - OTHER    : pigment + slurry 同居 (16k mb base each)
 *
 * 対応 tier: LV(1) 〜 UHV(9). ULV は実用性低いため除外.
 */
public final class ChemicalHatches {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final int[] TIERS = {
            GTValues.LV, GTValues.MV, GTValues.HV, GTValues.EV, GTValues.IV,
            GTValues.LuV, GTValues.ZPM, GTValues.UV, GTValues.UHV
    };

    /** 外部参照用: (variant) -> (io) -> (tier) -> MachineDefinition */
    public static final Map<ChemicalHatchVariant, Map<IO, Map<Integer, MachineDefinition>>> LOOKUP
            = new EnumMap<>(ChemicalHatchVariant.class);

    /** Creative 段のみ: (variant) -> (io) -> MachineDefinition */
    public static final Map<ChemicalHatchVariant, Map<IO, MachineDefinition>> CREATIVE_LOOKUP
            = new EnumMap<>(ChemicalHatchVariant.class);

    /** 創造タブ登録などで全machineを一括走査するためのフラット list */
    public static final java.util.List<MachineDefinition> ALL = new java.util.ArrayList<>();

    private ChemicalHatches() {}

    public static void init() {
        LOGGER.info("[ChemCap] === Registering chemical IO hatches (3 variants × 2 IO × {} tiers + creative) ===", TIERS.length);
        int count = 0;
        for (ChemicalHatchVariant variant : ChemicalHatchVariant.values()) {
            Map<IO, Map<Integer, MachineDefinition>> ioMap = new EnumMap<>(IO.class);
            Map<IO, MachineDefinition> creativeIoMap = new EnumMap<>(IO.class);
            for (IO io : new IO[]{IO.IN, IO.OUT}) {
                Map<Integer, MachineDefinition> tierMap = new HashMap<>();
                for (int tier : TIERS) {
                    MachineDefinition def = registerSingle(variant, io, tier);
                    tierMap.put(tier, def);
                    ALL.add(def);
                    count++;
                }
                MachineDefinition creativeDef = registerCreative(variant, io);
                ALL.add(creativeDef);
                creativeIoMap.put(io, creativeDef);
                count++;
                ioMap.put(io, tierMap);
            }
            LOOKUP.put(variant, ioMap);
            CREATIVE_LOOKUP.put(variant, creativeIoMap);
            LOGGER.info("[ChemCap]   variant={} registered {} machines ({} tiers + creative × 2 IO)",
                    variant, (TIERS.length + 1) * 2, TIERS.length);
        }
        LOGGER.info("[ChemCap] === Hatches registered: {} machines total ===", count);
    }

    private static MachineDefinition registerSingle(ChemicalHatchVariant variant, IO io, int tier) {
        String ioKey   = io == IO.IN ? "input" : "output";
        String tierKey = GTValues.VN[tier].toLowerCase(Locale.ROOT);
        String name    = variant.key + "_" + ioKey + "_hatch_" + tierKey;

        PartAbility[] abilities = variant.types.stream()
                .map(t -> ChemicalPartAbilities.get(io, t))
                .toArray(PartAbility[]::new);

        LOGGER.debug("[ChemCap] registerSingle name={} abilities={} tier={} capacity={}",
                name, abilities.length, tier, variant.baseCapacity * (1L << Math.min(9, tier)));

        return ModMachines.REGISTRATE.machine(name,
                        holder -> new ChemicalIOHatchMachine(holder, tier, io, variant))
                .rotationState(RotationState.ALL)
                .tier(tier)
                .abilities(abilities)
                .tooltips(
                        Component.translatable(
                                "gtcsolo.machine.chemical_hatch.desc.1",
                                tierCapacityString(tier, variant.baseCapacity)),
                        Component.translatable(
                                "gtcsolo.machine.chemical_hatch.desc.type." + variant.key),
                        Component.translatable(
                                "gtcsolo.machine.chemical_hatch.desc.io." + ioKey))
                .overlayTieredHullRenderer("chemical_hatch_" + variant.key + "_" + ioKey)
                .register();
    }

    private static MachineDefinition registerCreative(ChemicalHatchVariant variant, IO io) {
        String ioKey = io == IO.IN ? "input" : "output";
        String name  = variant.key + "_" + ioKey + "_hatch_creative";
        long capacity = (long) Integer.MAX_VALUE;

        PartAbility[] abilities = variant.types.stream()
                .map(t -> ChemicalPartAbilities.get(io, t))
                .toArray(PartAbility[]::new);

        LOGGER.debug("[ChemCap] registerCreative name={} abilities={} capacity={}",
                name, abilities.length, capacity);

        return ModMachines.REGISTRATE.machine(name,
                        holder -> new ChemicalIOHatchMachine(holder, GTValues.MAX, io, variant, capacity))
                .rotationState(RotationState.ALL)
                .tier(GTValues.MAX)
                .abilities(abilities)
                .tooltips(
                        Component.translatable("gtcsolo.machine.chemical_hatch.desc.creative"),
                        Component.translatable("gtcsolo.machine.chemical_hatch.desc.type." + variant.key),
                        Component.translatable("gtcsolo.machine.chemical_hatch.desc.io." + ioKey))
                .overlayTieredHullRenderer("chemical_hatch_" + variant.key + "_" + ioKey + "_creative")
                .register();
    }

    private static String tierCapacityString(int tier, long baseCapacity) {
        long cap = baseCapacity * (1L << Math.min(9, tier));
        if (cap >= 1_000_000L) return (cap / 1_000_000L) + "M mb";
        if (cap >= 1_000L)     return (cap / 1_000L)     + "k mb";
        return cap + " mb";
    }
}