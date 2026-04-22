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
 * Mekanism chemical IO hatch 4 種 × IO 2 種 × tier 9 段階 = 72 machine 登録.
 *
 * 対応 tier: LV(1) 〜 UHV(9). ULV は実用性低いため除外.
 * 基本容量: 16000 mb. tier 別に baseCapacity * 2^min(tier, 9) でスケール.
 *
 * 登録されたマシンは TYPE_IO_MACHINES テーブル (chemType × io × tier) で検索可能.
 */
public final class ChemicalHatches {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** tier 0(ULV)は除外、tier 1(LV) 〜 9(UHV). */
    private static final int[] TIERS = {
            GTValues.LV, GTValues.MV, GTValues.HV, GTValues.EV, GTValues.IV,
            GTValues.LuV, GTValues.ZPM, GTValues.UV, GTValues.UHV
    };

    /** GAS/PIGMENT/SLURRY の基本容量 (mb). tier 1 で 32000 から始まり UHV で 16M. */
    private static final long BASE_CAPACITY_MB = 16_000L;
    /** INFUSION は少量で使われる (metallurgic infuser の内部タンク相当) */
    private static final long BASE_CAPACITY_INFUSION = 4_000L;

    /** 外部参照用: (type) -> (io) -> (tier) -> MachineDefinition */
    public static final Map<ChemicalIngredient.Type, Map<IO, Map<Integer, MachineDefinition>>> LOOKUP
            = new EnumMap<>(ChemicalIngredient.Type.class);

    /** 創造タブ登録などで全machineを一括走査するためのフラット list */
    public static final java.util.List<MachineDefinition> ALL = new java.util.ArrayList<>();

    private ChemicalHatches() {}

    public static void init() {
        LOGGER.info("[ChemCap] === Registering chemical IO hatches (4 types × 2 IO × {} tiers) ===", TIERS.length);
        int count = 0;
        for (ChemicalIngredient.Type type : ChemicalIngredient.Type.values()) {
            Map<IO, Map<Integer, MachineDefinition>> ioMap = new EnumMap<>(IO.class);
            for (IO io : new IO[]{IO.IN, IO.OUT}) {
                Map<Integer, MachineDefinition> tierMap = new HashMap<>();
                for (int tier : TIERS) {
                    MachineDefinition def = registerSingle(type, io, tier);
                    tierMap.put(tier, def);
                    ALL.add(def);
                    count++;
                }
                ioMap.put(io, tierMap);
            }
            LOOKUP.put(type, ioMap);
            LOGGER.info("[ChemCap]   type={} registered {} machines ({} tiers × 2 IO)",
                    type, TIERS.length * 2, TIERS.length);
        }
        LOGGER.info("[ChemCap] === Hatches registered: {} machines total ===", count);
    }

    private static MachineDefinition registerSingle(ChemicalIngredient.Type type, IO io, int tier) {
        String typeKey = type.lowerName();                     // gas / infusion / pigment / slurry
        String ioKey   = io == IO.IN ? "input" : "output";
        String tierKey = GTValues.VN[tier].toLowerCase(Locale.ROOT);
        String name    = typeKey + "_" + ioKey + "_hatch_" + tierKey;  // e.g. gas_input_hatch_lv

        long baseCapacity = type == ChemicalIngredient.Type.INFUSION
                ? BASE_CAPACITY_INFUSION : BASE_CAPACITY_MB;

        PartAbility ability = ChemicalPartAbilities.get(io, type);
        LOGGER.debug("[ChemCap] registerSingle name={} ability={}_{} tier={} capacity={}",
                name, ioKey, typeKey, tier, baseCapacity * (1L << Math.min(9, tier)));

        return ModMachines.REGISTRATE.machine(name,
                        holder -> new ChemicalIOHatchMachine(holder, tier, io, type, baseCapacity))
                .rotationState(RotationState.ALL)
                .tier(tier)
                .abilities(ability)
                .tooltips(
                        Component.translatable(
                                "gtcsolo.machine.chemical_hatch.desc.1",
                                tierCapacityString(tier, baseCapacity)),
                        Component.translatable(
                                "gtcsolo.machine.chemical_hatch.desc.type." + typeKey),
                        Component.translatable(
                                "gtcsolo.machine.chemical_hatch.desc.io." + ioKey))
                .overlayTieredHullRenderer("chemical_hatch_" + typeKey + "_" + ioKey)
                .register();
    }

    private static String tierCapacityString(int tier, long baseCapacity) {
        long cap = baseCapacity * (1L << Math.min(9, tier));
        if (cap >= 1_000_000L) return (cap / 1_000_000L) + "M mb";
        if (cap >= 1_000L)     return (cap / 1_000L)     + "k mb";
        return cap + " mb";
    }
}