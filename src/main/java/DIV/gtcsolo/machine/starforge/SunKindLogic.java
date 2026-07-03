package DIV.gtcsolo.machine.starforge;

import DIV.gtcsolo.item.AbstractLocusItem;
import DIV.gtcsolo.registry.ModItems;
import com.gregtechceu.gtceu.api.GTValues;
import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

/**
 * MATURITY_SUN (= 太陽軌跡) 用の Strategy。
 * 仕様: docs/StarForge_spec.md §5
 */
public final class SunKindLogic implements StarForgeKindLogic {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final SunKindLogic INSTANCE = new SunKindLogic();
    private SunKindLogic() {}

    /** 成熟フェイズ duration (= 1200 秒 = 24,000 tick) */
    public static final long MATURITY_DURATION = 24_000L;
    /** 必要 UV Solar Panel 個数 */
    public static final int SOLAR_REQUIREMENT = 328;
    /** 搬入完了後の放出 EU/t (= UHV 32A = 131,072 × 32) */
    public static final long SUN_OUTPUT_EUT = GTValues.V[GTValues.UHV] * 32L;
    /** 崩壊時に還元する UV Solar Panel 個数 (= 搬入総数の 2 割) */
    public static final int SOLAR_REFUND = 65;

    private static Item solarPanelItem;
    private static Item getSolarPanel() {
        if (solarPanelItem == null) {
            solarPanelItem = BuiltInRegistries.ITEM.get(new ResourceLocation("gtceu", "uv_solar_panel"));
            LOGGER.debug("[StarForge:Sun] resolved UV Solar Panel item: {}", solarPanelItem);
        }
        return solarPanelItem;
    }

    @Override
    public StarForgeMachine.Phase nextPhaseAfterBuild() {
        LOGGER.debug("[StarForge:Sun] BUILD complete -> MATURITY (1200s timer)");
        return StarForgeMachine.Phase.MATURITY;
    }

    @Override
    public void onMaturityStart(StarForgeMachine machine, StarForgeTraceData.TraceInfo info) {
        LOGGER.debug("[StarForge:Sun:{}] MATURITY started, awaiting {} UV Solar Panels",
                info.trace, SOLAR_REQUIREMENT);
    }

    @Override
    public void onDecayStart(StarForgeMachine machine, StarForgeTraceData.TraceInfo info) {
        LOGGER.debug("[StarForge:Sun:{}] DECAY started (single tick)", info.trace);
    }

    @Override
    public boolean tickMaturity(StarForgeMachine machine, StarForgeTraceData.TraceInfo info) {
        machine.addMaturityElapsed(1);
        int solarCount = machine.getSolarPanelCount();
        if (solarCount < SOLAR_REQUIREMENT) {
            Item solar = getSolarPanel();
            if (solar != null) {
                int need = SOLAR_REQUIREMENT - solarCount;
                int consumed = machine.consumeInputItems(stack -> stack.is(solar), need);
                if (consumed > 0) {
                    machine.addSolarPanelCount(consumed);
                }
            }
        } else {
            machine.emitEnergyToOutput(SUN_OUTPUT_EUT);
            // 細かいログ抑制: 200 tick (= 10 秒) おきにのみ
            if (machine.getMaturityElapsed() % 200 == 0) {
                LOGGER.debug("[StarForge:Sun:{}] emitting {} EU/t (elapsed {}/{})",
                        info.trace, SUN_OUTPUT_EUT,
                        machine.getMaturityElapsed(), MATURITY_DURATION);
            }
        }
        if (machine.getMaturityElapsed() >= MATURITY_DURATION) {
            LOGGER.debug("[StarForge:Sun:{}] MATURITY complete -> DECAY", info.trace);
            return true;
        }
        return false;
    }

    @Override
    public DecayResult tickDecay(StarForgeMachine machine, StarForgeTraceData.TraceInfo info) {
        LOGGER.debug("[StarForge:Sun:{}] DECAY tick (single, immediate success)", info.trace);
        return DecayResult.SUCCESS;
    }

    @Override
    public void emitOutputsOnSuccess(StarForgeMachine machine, StarForgeTraceData.TraceInfo info) {
        // 注: decaying_star_locus は orchestrator が先に emitDecayingLocus で出力済
        machine.emitCommonOutputs(info);
        // 追加出力: UV Solar Panel × 65 (= 還元)
        Item solar = getSolarPanel();
        if (solar != null) {
            machine.outputItem(new ItemStack(solar, SOLAR_REFUND));
            LOGGER.debug("[StarForge:Sun:{}] emit refund: {} x UV Solar Panel",
                    info.trace, SOLAR_REFUND);
        }
    }
}
