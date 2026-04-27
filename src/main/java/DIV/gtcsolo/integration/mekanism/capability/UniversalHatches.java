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
 * Universal IO hatch 登録. 2 IO × 9 tiers = 18 machine.
 *
 * Universal = fluid か gas のどちらか片方を排他で保持可能.
 * 両 tank 同容量 (GT 標準 fluid hatch に準拠: 8000 × 2^tier mb).
 *
 * PartAbility: INPUT_FLUIDS + INPUT_GAS (入力用) または EXPORT_FLUIDS + OUTPUT_GAS (出力用).
 * どちらの枠にも置けるため既存 GT fluid 要求マルチ + 我々の gas 要求マルチ両対応.
 */
public final class UniversalHatches {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final int[] TIERS = {
            GTValues.LV, GTValues.MV, GTValues.HV, GTValues.EV, GTValues.IV,
            GTValues.LuV, GTValues.ZPM, GTValues.UV, GTValues.UHV
    };

    public static final Map<IO, Map<Integer, MachineDefinition>> LOOKUP = new EnumMap<>(IO.class);
    public static final java.util.List<MachineDefinition> ALL = new java.util.ArrayList<>();

    private UniversalHatches() {}

    public static void init() {
        LOGGER.info("[ChemCap] === Registering universal IO hatches (2 IO × {} tiers) ===", TIERS.length);
        int count = 0;
        for (IO io : new IO[]{IO.IN, IO.OUT}) {
            Map<Integer, MachineDefinition> tierMap = new HashMap<>();
            for (int tier : TIERS) {
                MachineDefinition def = registerSingle(io, tier);
                tierMap.put(tier, def);
                ALL.add(def);
                count++;
            }
            LOOKUP.put(io, tierMap);
        }
        LOGGER.info("[ChemCap] === Universal hatches registered: {} machines total ===", count);
    }

    private static MachineDefinition registerSingle(IO io, int tier) {
        String ioKey   = io == IO.IN ? "input" : "output";
        String tierKey = GTValues.VN[tier].toLowerCase(Locale.ROOT);
        String name    = "universal_" + ioKey + "_hatch_" + tierKey;

        PartAbility fluidAbility = io == IO.IN ? PartAbility.IMPORT_FLUIDS : PartAbility.EXPORT_FLUIDS;
        PartAbility chemAbility  = ChemicalPartAbilities.get(io, ChemicalIngredient.Type.GAS);

        long capacity = 8000L * (1L << Math.min(9, tier));

        return ModMachines.REGISTRATE.machine(name,
                        holder -> new UniversalIOHatchMachine(holder, tier, io))
                .rotationState(RotationState.ALL)
                .tier(tier)
                .abilities(fluidAbility, chemAbility)
                .tooltips(
                        Component.translatable(
                                "gtcsolo.machine.universal_hatch.desc.1",
                                tierCapacityString(capacity)),
                        Component.translatable("gtcsolo.machine.universal_hatch.desc.exclusive"),
                        Component.translatable(
                                "gtcsolo.machine.chemical_hatch.desc.io." + ioKey))
                .overlayTieredHullRenderer("universal_hatch_" + ioKey)
                .register();
    }

    private static String tierCapacityString(long cap) {
        if (cap >= 1_000_000L) return (cap / 1_000_000L) + "M mb";
        if (cap >= 1_000L)     return (cap / 1_000L)     + "k mb";
        return cap + " mb";
    }
}