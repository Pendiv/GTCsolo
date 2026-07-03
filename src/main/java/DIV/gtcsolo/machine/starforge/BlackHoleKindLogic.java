package DIV.gtcsolo.machine.starforge;

import DIV.gtcsolo.registry.ModMaterials;
import com.gregtechceu.gtceu.api.data.chemical.ChemicalHelper;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

import java.util.Random;

/**
 * MATURITY_BLACK_HOLE (= ブラックホール軌跡) 用の Strategy。
 * 仕様: docs/StarForge_spec.md §6
 *
 * <ul>
 *   <li>成熟 (1200 tick): Neutronium Block 投入 → 20 tick 連鎖式に MAX+1V × 2A 放出</li>
 *   <li>崩壊 (~100 sec): 崩壊度 100→0%、 ランダム閾値 1-92% で過消費発火</li>
 *   <li>過消費 160 tick: 最初 20 tick MAX+14V × 2A + 後 140 tick MAX+1V × 2A</li>
 *   <li>過消費中に working_enabled OFF → singularity 獲得 + 成功終了</li>
 *   <li>過消費未検知で崩壊度 0% 到達 → 失敗 (出力 0)</li>
 * </ul>
 */
public final class BlackHoleKindLogic implements StarForgeKindLogic {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final BlackHoleKindLogic INSTANCE = new BlackHoleKindLogic();
    private BlackHoleKindLogic() {}

    /** 成熟フェイズ duration (= 1200 tick = 60 秒) */
    public static final long MATURITY_DURATION = 1_200L;
    /** 物体投入 1 個あたりの放出 EU/t (= MAX+1V × 2A = 2^33) */
    public static final long BH_EMISSION_EUT = 1L << 33;
    /** 物体投入 1 個あたりの放出 tick */
    public static final int BH_EMISSION_TICKS_PER_BLOCK = 20;

    /** 崩壊フェイズで 1% 減衰に要する tick (= 1 秒 = 20 tick) */
    public static final int DECAY_TICKS_PER_PERCENT = 20;
    /** 過消費の閾値ランダム値域 (= 1〜92 inclusive) */
    public static final int SURGE_THRESHOLD_MIN = 1;
    public static final int SURGE_THRESHOLD_MAX = 92;

    /** 過消費の総 tick (= 160 tick = 8 秒) */
    public static final int SURGE_TOTAL_TICKS = 160;
    /** 過消費の高電力期間 (= 最初 20 tick MAX+14V × 2A = 2^62 EU/t) */
    public static final int SURGE_PHASE1_TICKS = 20;
    public static final long SURGE_PHASE1_EUT = 1L << 62;
    /** 過消費の通常電力期間 (= 残り 140 tick、 MAX+1V × 2A = 2^34 EU/t) */
    public static final long SURGE_PHASE2_EUT = 1L << 34;

    private static final Random RANDOM = new Random();
    private static Item neutroniumBlockItem;

    private static Item getNeutroniumBlock() {
        if (neutroniumBlockItem == null) {
            neutroniumBlockItem = BuiltInRegistries.ITEM.get(new ResourceLocation("gtceu", "neutronium_block"));
            LOGGER.debug("[StarForge:BH] resolved Neutronium Block item: {}", neutroniumBlockItem);
        }
        return neutroniumBlockItem;
    }

    @Override
    public StarForgeMachine.Phase nextPhaseAfterBuild() {
        LOGGER.debug("[StarForge:BH] BUILD complete -> MATURITY (1200 tick)");
        return StarForgeMachine.Phase.MATURITY;
    }

    @Override
    public void onMaturityStart(StarForgeMachine machine, StarForgeTraceData.TraceInfo info) {
        LOGGER.debug("[StarForge:BH:{}] MATURITY started, accept Neutronium Block (1200 tick window)",
                info.trace);
    }

    @Override
    public void onDecayStart(StarForgeMachine machine, StarForgeTraceData.TraceInfo info) {
        // 崩壊フェイズ開始: 崩壊度 100、 閾値ランダム生成
        machine.setBhDecayPercent(100.0);
        int threshold = SURGE_THRESHOLD_MIN + RANDOM.nextInt(SURGE_THRESHOLD_MAX - SURGE_THRESHOLD_MIN + 1);
        machine.setBhSurgeThreshold(threshold);
        machine.setBhSurgeTicksRemaining(0);
        machine.setBhSurgeStarted(false);
        LOGGER.debug("[StarForge:BH:{}] DECAY started, threshold = {}%, decay = 100.00%",
                info.trace, threshold);
    }

    @Override
    public boolean tickMaturity(StarForgeMachine machine, StarForgeTraceData.TraceInfo info) {
        machine.addMaturityElapsed(1);

        // Neutronium Block 消費 (= 連鎖式に放出キュー追加)
        Item nb = getNeutroniumBlock();
        if (nb != null) {
            // 連鎖上限の安全弁: 残り maturity tick 数を超えない範囲で吸う
            long remainingMaturity = MATURITY_DURATION - machine.getMaturityElapsed();
            int maxBlocks = (int) Math.max(0, remainingMaturity / BH_EMISSION_TICKS_PER_BLOCK);
            if (maxBlocks > 0) {
                int consumed = machine.consumeInputItems(stack -> stack.is(nb), maxBlocks);
                if (consumed > 0) {
                    machine.addBhEmissionTicks(consumed * BH_EMISSION_TICKS_PER_BLOCK);
                }
            }
        }

        // 放出キューがあれば 1 tick 分放出
        if (machine.getBhEmissionTicks() > 0) {
            machine.emitEnergyToOutput(BH_EMISSION_EUT);
            machine.addBhEmissionTicks(-1);
            if (machine.getMaturityElapsed() % 20 == 0) {
                LOGGER.debug("[StarForge:BH:{}] emitting {} EU/t (queue {}, elapsed {}/{})",
                        info.trace, BH_EMISSION_EUT, machine.getBhEmissionTicks(),
                        machine.getMaturityElapsed(), MATURITY_DURATION);
            }
        }

        if (machine.getMaturityElapsed() >= MATURITY_DURATION) {
            LOGGER.debug("[StarForge:BH:{}] MATURITY complete -> DECAY", info.trace);
            return true;
        }
        return false;
    }

    /**
     * BH 崩壊フェイズの 1 tick。
     * 注: 電源 OFF 検知は orchestrator 側で先に行われ、 ここに到達するのは working_enabled = true の時。
     */
    @Override
    public DecayResult tickDecay(StarForgeMachine machine, StarForgeTraceData.TraceInfo info) {
        double decayPercent = machine.getBhDecayPercent();
        int surgeRemain = machine.getBhSurgeTicksRemaining();
        int threshold = machine.getBhSurgeThreshold();

        // 過消費中: 電力消費 + 残り tick 減少
        if (surgeRemain > 0) {
            int elapsedInSurge = SURGE_TOTAL_TICKS - surgeRemain;
            long requiredEut = (elapsedInSurge < SURGE_PHASE1_TICKS)
                    ? SURGE_PHASE1_EUT : SURGE_PHASE2_EUT;
            consumeEnergyForSurge(machine, requiredEut);
            if (surgeRemain % 20 == 0) {
                LOGGER.debug("[StarForge:BH:{}] SURGE tick {}/{}, EUt required = {}, decay = {}%",
                        info.trace, elapsedInSurge, SURGE_TOTAL_TICKS,
                        requiredEut, String.format("%.2f", decayPercent));
            }
            machine.addBhSurgeTicksRemaining(-1);
        }

        // 崩壊度減衰 (1%/20 tick = 0.05%/tick)
        double newDecay = decayPercent - (100.0 / (DECAY_TICKS_PER_PERCENT * 100.0));
        machine.setBhDecayPercent(newDecay);

        // 過消費発火判定 (一度だけ)
        if (!machine.isBhSurgeStarted() && newDecay <= threshold) {
            machine.setBhSurgeTicksRemaining(SURGE_TOTAL_TICKS);
            machine.setBhSurgeStarted(true);
            LOGGER.debug("[StarForge:BH:{}] SURGE triggered at decay={}% (threshold={}%)",
                    info.trace, String.format("%.2f", newDecay), threshold);
        }

        // 崩壊度ログ (10 秒おき = 200 tick おき)
        long elapsedDecayTicks = (long) ((100.0 - newDecay) * DECAY_TICKS_PER_PERCENT);
        if (elapsedDecayTicks % 200 == 0 && elapsedDecayTicks > 0) {
            LOGGER.debug("[StarForge:BH:{}] decay = {}%, threshold = {}%, surge_started = {}",
                    info.trace, String.format("%.2f", newDecay), threshold, machine.isBhSurgeStarted());
        }

        // 崩壊度 0% で失敗 (過消費を検知せずに完走)
        if (newDecay <= 0.0) {
            LOGGER.warn("[StarForge:BH:{}] DECAY reached 0% without surge-OFF -> FAILURE", info.trace);
            return DecayResult.FAILURE;
        }
        return DecayResult.CONTINUE;
    }

    /**
     * Orchestrator から呼ばれる「過消費中の working_enabled OFF 検知」 ハンドラ。
     * 成功完了 (= singularity 獲得) を orchestrator に通知する。
     * @return true なら singularity 獲得成功扱い、 false なら無視
     */
    public boolean handlePowerOffDuringSurge(StarForgeMachine machine, StarForgeTraceData.TraceInfo info) {
        if (machine.getBhSurgeTicksRemaining() <= 0) return false;
        LOGGER.debug("[StarForge:BH:{}] Power OFF during surge -> SUCCESS (singularity granted)",
                info.trace);
        return true;
    }

    @Override
    public void emitDecayingLocus(StarForgeMachine machine, StarForgeTraceData.TraceInfo info) {
        // BH 成功時は朽ち果てた星の軌跡を返さない (= singularity 獲得が報酬の全て)
    }

    @Override
    public void emitOutputsOnSuccess(StarForgeMachine machine, StarForgeTraceData.TraceInfo info) {
        // BH 成功時は star_singularity のみ。 既存 outputItems / outputFluids は出さない
        ItemStack singularity = ChemicalHelper.get(TagPrefix.ingot, ModMaterials.STAR_SINGULARITY, 1);
        machine.outputItem(singularity);
        LOGGER.debug("[StarForge:BH:{}] emit star_singularity (success path)", info.trace);
    }

    /**
     * 過消費中のエネルギー消費。 接続電線 (= controller の energyContainer) から
     * 「取れるだけ」 吸い上げる。 不足分はそのまま (= 出力電力は player の電力網次第)。
     */
    private void consumeEnergyForSurge(StarForgeMachine machine, long requiredEut) {
        machine.tryConsumeEnergyFromContainer(requiredEut);
    }
}
