package DIV.gtcsolo.registry;

import DIV.gtcsolo.machine.wen.WENEnergyHatchMachine;
import DIV.gtcsolo.machine.wen.WENEnergyOutputHatchMachine;
import DIV.gtcsolo.machine.wen.WENWirelessInputMachine;
import DIV.gtcsolo.machine.wen.WENWirelessOutputMachine;
import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.data.RotationState;
import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;
import com.gregtechceu.gtceu.common.machine.multiblock.part.EnergyHatchPartMachine;
import com.mojang.logging.LogUtils;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static DIV.gtcsolo.registry.ModMachines.REGISTRATE;

/**
 * WEN ワイヤレスエネルギーネットワーク — マシン登録
 * EV(4)~MAX(14) × 1A,4A,16A,64A,256A
 * Input: 55マシン + Output: 55マシン = 110マシン
 */
public class WENMachines {

    private static final Logger LOGGER = LogUtils.getLogger();

    public static final Map<Integer, Map<Integer, MachineDefinition>> WIRELESS_INPUT = new HashMap<>();
    public static final Map<Integer, Map<Integer, MachineDefinition>> WIRELESS_OUTPUT = new HashMap<>();
    public static final Map<Integer, Map<Integer, MachineDefinition>> ENERGY_HATCH = new HashMap<>();
    public static final Map<Integer, Map<Integer, MachineDefinition>> ENERGY_OUTPUT_HATCH = new HashMap<>();

    private static final int[] AMPERAGES = {1, 4, 16, 64, 256};
    /** エネルギーハッチ専用の追加高アンペア */
    private static final int[] HATCH_EXTRA_AMPS = {1024, 4096, 16384};
    /** 出力ハッチのアンペア（画像に対応: 1,4,16,64,256,1024,2048） */
    private static final int[] OUTPUT_HATCH_AMPS = {1, 4, 16, 64, 256, 1024, 2048};
    private static final int MIN_TIER = GTValues.EV;
    private static final int MAX_TIER = GTValues.MAX;

    public static void init() {
        for (int tier = MIN_TIER; tier <= MAX_TIER; tier++) {
            WIRELESS_INPUT.put(tier, new HashMap<>());
            WIRELESS_OUTPUT.put(tier, new HashMap<>());
            ENERGY_HATCH.put(tier, new HashMap<>());
            ENERGY_OUTPUT_HATCH.put(tier, new HashMap<>());
            for (int amp : AMPERAGES) {
                registerWirelessInput(tier, amp);
                registerWirelessOutput(tier, amp);
                registerEnergyHatch(tier, amp);
            }
            // エネルギーハッチのみ高アンペア追加
            for (int amp : HATCH_EXTRA_AMPS) {
                registerEnergyHatch(tier, amp);
            }
            // エネルギー出力ハッチ（ダイナモ）
            for (int amp : OUTPUT_HATCH_AMPS) {
                registerEnergyOutputHatch(tier, amp);
            }
        }
        int tierCount = MAX_TIER - MIN_TIER + 1;
        int ioCount = tierCount * AMPERAGES.length;
        int hatchCount = tierCount * (AMPERAGES.length + HATCH_EXTRA_AMPS.length);
        LOGGER.info("[WEN] Registered {} input + {} output + {} energy hatch = {} machines",
                ioCount, ioCount, hatchCount, ioCount * 2 + hatchCount);
    }

    /** アンペア数を表示用サフィックスに変換 (1024→1ka, 4096→4k, 16384→16k, それ以外→{n}a) */
    private static String getAmpSuffix(int amperage) {
        return switch (amperage) {
            case 1024 -> "1ka";
            case 4096 -> "4k";
            case 16384 -> "16k";
            default -> amperage + "a";
        };
    }

    /** アンペア数を表示用文字列に変換 */
    private static String getAmpDisplay(int amperage) {
        return switch (amperage) {
            case 1024 -> "1,024";
            case 4096 -> "4,096";
            case 16384 -> "16,384";
            default -> String.valueOf(amperage);
        };
    }

    private static void registerWirelessInput(int tier, int amperage) {
        String tierName = GTValues.VN[tier].toLowerCase(Locale.ROOT);
        String name = "wen_wireless_input_" + tierName + "_" + amperage + "a";
        String overlayPath = "wen_wireless_input_" + amperage + "a";

        MachineDefinition def = REGISTRATE.machine(name,
                        holder -> WENWirelessInputMachine.create(holder, tier, amperage))
                .rotationState(RotationState.NON_Y_AXIS)
                .tier(tier)
                .tooltips(
                        Component.translatable("gtcsolo.machine.wen_wireless_input.desc",
                                GTValues.VNF[tier], amperage),
                        Component.translatable("gtcsolo.machine.wen_wireless_input.desc2"))
                .overlayTieredHullRenderer(overlayPath)
                .register();

        WIRELESS_INPUT.get(tier).put(amperage, def);
    }

    private static void registerEnergyHatch(int tier, int amperage) {
        String tierName = GTValues.VN[tier].toLowerCase(Locale.ROOT);
        String ampSuffix = getAmpSuffix(amperage);
        String name = "wen_energy_hatch_" + tierName + "_" + ampSuffix;
        String overlayPath = "wen_wireless_input_" + ampSuffix;

        MachineDefinition def = REGISTRATE.machine(name,
                        holder -> WENEnergyHatchMachine.create(holder, tier, amperage))
                .rotationState(RotationState.ALL)
                .tier(tier)
                .abilities(PartAbility.INPUT_ENERGY) // GTマルチのエネルギーハッチとして認識
                .tooltips(
                        Component.translatable("gtcsolo.machine.wen_energy_hatch.desc",
                                GTValues.VNF[tier], getAmpDisplay(amperage)),
                        Component.translatable("gtcsolo.machine.wen_energy_hatch.desc2"),
                        Component.translatable("gtceu.universal.tooltip.energy_storage_capacity",
                                com.gregtechceu.gtceu.utils.FormattingUtil.formatNumbers(
                                        EnergyHatchPartMachine.getHatchEnergyCapacity(tier, amperage))))
                .overlayTieredHullRenderer(overlayPath)
                .register();

        ENERGY_HATCH.get(tier).put(amperage, def);
    }

    private static void registerEnergyOutputHatch(int tier, int amperage) {
        String tierName = GTValues.VN[tier].toLowerCase(Locale.ROOT);
        String name = "wen_energy_output_hatch_" + tierName + "_" + amperage + "a";
        String overlayPath = "output_" + amperage + "a";

        MachineDefinition def = REGISTRATE.machine(name,
                        holder -> WENEnergyOutputHatchMachine.create(holder, tier, amperage))
                .rotationState(RotationState.ALL)
                .tier(tier)
                .abilities(PartAbility.OUTPUT_ENERGY)
                .tooltips(
                        Component.translatable("gtcsolo.machine.wen_energy_output_hatch.desc",
                                GTValues.VNF[tier], getAmpDisplay(amperage)),
                        Component.translatable("gtcsolo.machine.wen_energy_output_hatch.desc2"),
                        Component.translatable("gtceu.universal.tooltip.energy_storage_capacity",
                                com.gregtechceu.gtceu.utils.FormattingUtil.formatNumbers(
                                        EnergyHatchPartMachine.getHatchEnergyCapacity(tier, amperage))))
                .overlayTieredHullRenderer(overlayPath)
                .register();

        ENERGY_OUTPUT_HATCH.get(tier).put(amperage, def);
    }

    private static void registerWirelessOutput(int tier, int amperage) {
        String tierName = GTValues.VN[tier].toLowerCase(Locale.ROOT);
        String name = "wen_wireless_output_" + tierName + "_" + amperage + "a";
        String overlayPath = "wen_wireless_output_" + amperage + "a";

        MachineDefinition def = REGISTRATE.machine(name,
                        holder -> WENWirelessOutputMachine.create(holder, tier, amperage))
                .rotationState(RotationState.NON_Y_AXIS)
                .tier(tier)
                .tooltips(
                        Component.translatable("gtcsolo.machine.wen_wireless_output.desc",
                                GTValues.VNF[tier], amperage),
                        Component.translatable("gtcsolo.machine.wen_wireless_output.desc2"))
                .overlayTieredHullRenderer(overlayPath)
                .register();

        WIRELESS_OUTPUT.get(tier).put(amperage, def);
    }
}