package DIV.gtcsolo.machine.starforge;

import DIV.gtcsolo.item.AbstractLocusItem;
import DIV.gtcsolo.registry.ModItems;
import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.capability.recipe.IRecipeHandler;
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.TickableSubscription;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiPart;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableEnergyContainer;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableFluidTank;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableItemStackHandler;
import com.gregtechceu.gtceu.common.machine.multiblock.part.EnergyHatchPartMachine;
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;
import com.mojang.logging.LogUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * StarForge 本体マシン (恒星鍛造炉)。
 * 詳細: docs/StarForge_spec.md ver.0.6
 *
 * <p>State: {@code IDLE → BUILD → (MATURITY) → DECAY → IDLE}
 */
public class StarForgeMachine extends WorkableElectricMultiblockMachine {

    private static final Logger LOGGER = LogUtils.getLogger();

    public static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            StarForgeMachine.class, WorkableElectricMultiblockMachine.MANAGED_FIELD_HOLDER);

    public enum Phase { IDLE, BUILD, MATURITY, DECAY }

    // =========================================================================
    //  Sync fields
    // =========================================================================

    @Persisted @DescSynced private Phase phase = Phase.IDLE;
    @Persisted @DescSynced private String currentTrace = null;
    @Persisted @DescSynced private long buildProgress = 0L;
    @Persisted @DescSynced private long decayProgress = 0L;
    @Persisted @DescSynced private long maturityElapsed = 0L;
    @Persisted @DescSynced private int solarPanelCount = 0;

    // BH 専用 state
    @Persisted @DescSynced private double bhDecayPercent = 0.0;
    @Persisted @DescSynced private int bhSurgeThreshold = 0;
    @Persisted @DescSynced private int bhSurgeTicksRemaining = 0;
    @Persisted @DescSynced private boolean bhSurgeStarted = false;
    @Persisted @DescSynced private int bhEmissionTicks = 0;

    private TickableSubscription tickSub;

    public StarForgeMachine(IMachineBlockEntity holder) {
        super(holder);
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    // =========================================================================
    //  Lifecycle
    // =========================================================================

    @Override
    public void onStructureFormed() {
        super.onStructureFormed();
        if (tickSub == null) {
            tickSub = subscribeServerTick(this::onServerTick);
            LOGGER.info("[StarForge] structure formed, tick subscribed");
        }
    }

    @Override
    public void onStructureInvalid() {
        super.onStructureInvalid();
        if (tickSub != null) { unsubscribe(tickSub); tickSub = null; }
        // 進捗は保持 (= @Persisted フィールドは NBT 保存)、 構造再形成で tick 再開 → 進捗自動継続
        LOGGER.info("[StarForge] structure invalid, tick paused (phase={}, trace={}, build={}, decay={}, maturity={})",
                phase, currentTrace, buildProgress, decayProgress, maturityElapsed);
    }

    // =========================================================================
    //  Server tick — 状態遷移本体
    // =========================================================================

    private void onServerTick() {
        if (!isFormed()) return;

        // BH 特殊検知: 過消費中に working_enabled OFF → singularity 獲得成功
        // この処理は isWorkingEnabled() の early return より先
        if (phase == Phase.DECAY && bhSurgeTicksRemaining > 0 && !isWorkingEnabled()) {
            StarForgeTraceData.TraceInfo info = currentInfo();
            if (info != null && info.kind == StarForgeTraceData.Kind.MATURITY_BLACK_HOLE) {
                if (BlackHoleKindLogic.INSTANCE.handlePowerOffDuringSurge(this, info)) {
                    BlackHoleKindLogic.INSTANCE.emitOutputsOnSuccess(this, info);
                    resetPhase();
                    LOGGER.info("[StarForge] BH success: returning to IDLE (working_enabled stays OFF, player must re-enable)");
                    return;
                }
            }
        }

        if (!isWorkingEnabled()) return;

        switch (phase) {
            case IDLE -> tickIdle();
            case BUILD -> tickBuild();
            case MATURITY -> tickKindMaturity();
            case DECAY -> tickKindDecay();
        }
    }

    /** IDLE: star_locus 検出で BUILD 開始 */
    private void tickIdle() {
        for (IItemHandlerModifiable inv : findItemHandlers(IO.IN)) {
            for (int i = 0; i < inv.getSlots(); i++) {
                ItemStack stack = inv.getStackInSlot(i);
                if (stack.isEmpty()) continue;
                if (!stack.is(ModItems.STAR_LOCUS.get())) continue;
                String trace = AbstractLocusItem.getTrace(stack);
                if (trace == null) continue;
                StarForgeTraceData.TraceInfo info = StarForgeTraceData.get(trace);
                if (info == null) continue;
                inv.extractItem(i, 1, false);
                currentTrace = trace;
                phase = Phase.BUILD;
                buildProgress = 0L;
                decayProgress = 0L;
                maturityElapsed = 0L;
                solarPanelCount = 0;
                bhEmissionTicks = 0;
                StarForgeKindLogic.forKind(info.kind).onReset(this, info);
                LOGGER.info("[StarForge] IDLE -> BUILD: trace={}, kind={}, buildRequired={}",
                        trace, info.kind, info.buildRequiredCount);
                return;
            }
        }
    }

    /** BUILD: 共通 logic で進捗テーブル評価 */
    private void tickBuild() {
        StarForgeTraceData.TraceInfo info = currentInfo();
        if (info == null) { resetPhase(); return; }
        if (info.buildPhaseTable == null) {
            LOGGER.warn("[StarForge:{}] no buildPhaseTable — skipping BUILD", info.trace);
            transitionAfterBuild(info);
            return;
        }
        long gain = consumeAndProgressFromTable(info.buildPhaseTable, /*budget=*/64);
        if (gain > 0) {
            buildProgress += gain;
        }
        if (buildProgress >= info.buildRequiredCount) {
            LOGGER.info("[StarForge:{}] BUILD complete at progress={}", info.trace, buildProgress);
            transitionAfterBuild(info);
        }
    }

    private void transitionAfterBuild(StarForgeTraceData.TraceInfo info) {
        StarForgeKindLogic logic = StarForgeKindLogic.forKind(info.kind);
        phase = logic.nextPhaseAfterBuild();
        maturityElapsed = 0L;
        solarPanelCount = 0;
        bhEmissionTicks = 0;
        if (phase == Phase.MATURITY) {
            logic.onMaturityStart(this, info);
        } else if (phase == Phase.DECAY) {
            logic.onDecayStart(this, info);
        }
    }

    private void tickKindMaturity() {
        StarForgeTraceData.TraceInfo info = currentInfo();
        if (info == null) { resetPhase(); return; }
        StarForgeKindLogic logic = StarForgeKindLogic.forKind(info.kind);
        if (logic.tickMaturity(this, info)) {
            phase = Phase.DECAY;
            decayProgress = 0L;
            logic.onDecayStart(this, info);
        }
    }

    private void tickKindDecay() {
        StarForgeTraceData.TraceInfo info = currentInfo();
        if (info == null) { resetPhase(); return; }
        StarForgeKindLogic logic = StarForgeKindLogic.forKind(info.kind);
        StarForgeKindLogic.DecayResult result = logic.tickDecay(this, info);
        switch (result) {
            case SUCCESS -> {
                LOGGER.info("[StarForge:{}] DECAY SUCCESS -> emit decaying_locus first, then outputs", info.trace);
                logic.emitDecayingLocus(this, info);
                logic.emitOutputsOnSuccess(this, info);
                resetPhase();
            }
            case FAILURE -> {
                LOGGER.warn("[StarForge:{}] DECAY FAILURE -> reset to IDLE without outputs", info.trace);
                resetPhase();
            }
            case CONTINUE -> {} // no-op
        }
    }

    private void resetPhase() {
        phase = Phase.IDLE;
        currentTrace = null;
        buildProgress = 0L;
        decayProgress = 0L;
        maturityElapsed = 0L;
        solarPanelCount = 0;
        bhDecayPercent = 0.0;
        bhSurgeThreshold = 0;
        bhSurgeTicksRemaining = 0;
        bhSurgeStarted = false;
        bhEmissionTicks = 0;
    }

    private StarForgeTraceData.TraceInfo currentInfo() {
        return currentTrace == null ? null : StarForgeTraceData.get(currentTrace);
    }

    // =========================================================================
    //  Accessors (Strategy から呼ばれる)
    // =========================================================================

    public Phase getPhase() { return phase; }
    public String getCurrentTrace() { return currentTrace; }
    public long getBuildProgress() { return buildProgress; }
    public long getDecayProgress() { return decayProgress; }
    public long getMaturityElapsed() { return maturityElapsed; }
    public int getSolarPanelCount() { return solarPanelCount; }
    public double getBhDecayPercent() { return bhDecayPercent; }
    public int getBhSurgeThreshold() { return bhSurgeThreshold; }
    public int getBhSurgeTicksRemaining() { return bhSurgeTicksRemaining; }
    public boolean isBhSurgeStarted() { return bhSurgeStarted; }
    public int getBhEmissionTicks() { return bhEmissionTicks; }

    public void addBuildProgress(long n) { buildProgress = Math.max(0, buildProgress + n); }
    public void addDecayProgress(long n) { decayProgress = Math.max(0, decayProgress + n); }
    public void addMaturityElapsed(long n) { maturityElapsed = Math.max(0, maturityElapsed + n); }
    public void addSolarPanelCount(int n) { solarPanelCount = Math.max(0, solarPanelCount + n); }
    public void setBhDecayPercent(double v) { bhDecayPercent = v; }
    public void setBhSurgeThreshold(int v) { bhSurgeThreshold = v; }
    public void setBhSurgeTicksRemaining(int v) { bhSurgeTicksRemaining = Math.max(0, v); }
    public void addBhSurgeTicksRemaining(int n) { bhSurgeTicksRemaining = Math.max(0, bhSurgeTicksRemaining + n); }
    public void setBhSurgeStarted(boolean v) { bhSurgeStarted = v; }
    public void addBhEmissionTicks(int n) { bhEmissionTicks = Math.max(0, bhEmissionTicks + n); }

    // =========================================================================
    //  IO ヘルパ — Strategy から呼ばれる
    // =========================================================================

    public long consumeAndProgressFromTable(PhaseProgressionTable table, int budget) {
        if (table == null || budget <= 0) return 0L;
        long totalGain = 0L;
        int consumed = 0;
        outer:
        for (IItemHandlerModifiable inv : findItemHandlers(IO.IN)) {
            for (int i = 0; i < inv.getSlots(); i++) {
                if (consumed >= budget) break outer;
                ItemStack stack = inv.getStackInSlot(i);
                if (stack.isEmpty()) continue;
                long perItem = table.evaluate(stack);
                if (perItem <= 0) continue;
                int take = Math.min(stack.getCount(), budget - consumed);
                ItemStack extracted = inv.extractItem(i, take, false);
                if (!extracted.isEmpty()) {
                    int got = extracted.getCount();
                    totalGain += perItem * got;
                    consumed += got;
                }
            }
        }
        return totalGain;
    }

    public int consumeInputItems(Predicate<ItemStack> match, int max) {
        if (max <= 0) return 0;
        int consumed = 0;
        for (IItemHandlerModifiable inv : findItemHandlers(IO.IN)) {
            for (int i = 0; i < inv.getSlots() && consumed < max; i++) {
                ItemStack stack = inv.getStackInSlot(i);
                if (stack.isEmpty()) continue;
                if (!match.test(stack)) continue;
                int take = Math.min(stack.getCount(), max - consumed);
                ItemStack got = inv.extractItem(i, take, false);
                consumed += got.getCount();
            }
        }
        return consumed;
    }

    public void outputItem(ItemStack stack) {
        if (stack.isEmpty()) return;
        ItemStack remain = stack.copy();
        for (IItemHandlerModifiable inv : findItemHandlers(IO.OUT)) {
            for (int i = 0; i < inv.getSlots() && !remain.isEmpty(); i++) {
                remain = inv.insertItem(i, remain, false);
            }
            if (remain.isEmpty()) return;
        }
        if (!remain.isEmpty()) {
            LOGGER.warn("[StarForge] output bus full, lost {}x{}", remain.getCount(), remain.getItem());
        }
    }

    public void outputFluid(FluidStack fluid) {
        if (fluid == null || fluid.isEmpty()) return;
        FluidStack remain = fluid.copy();
        for (NotifiableFluidTank tank : findFluidTanks(IO.OUT)) {
            int filled = tank.fillInternal(remain, IFluidHandler.FluidAction.EXECUTE);
            remain.shrink(filled);
            if (remain.isEmpty()) return;
        }
        if (!remain.isEmpty()) {
            LOGGER.warn("[StarForge] fluid output full, lost {}mB of {}",
                    remain.getAmount(), remain.getFluid());
        }
    }

    /**
     * OUTPUT_ENERGY ハッチ群に EU を加算 (= ダイソンキューブ放出、 BH 物体放出など)。
     * 複数ハッチがあれば均等分配。 ハッチが無ければエネルギーは消失 + warn ログ。
     * @return 実際に分配できた EU 量
     */
    public long emitEnergyToOutput(long eut) {
        if (eut <= 0) return 0;
        List<NotifiableEnergyContainer> outs = findOutputEnergyContainers();
        if (outs.isEmpty()) {
            // 単発で大量ログ防止のため 200 tick おきにのみ
            if ((getOffsetTimer() % 200) == 0) {
                LOGGER.warn("[StarForge] no OUTPUT_ENERGY hatch found, {} EU/t lost (place an output energy hatch in 'E' slot)",
                        eut);
            }
            return 0;
        }
        long perContainer = eut / outs.size();
        long remainder = eut - perContainer * outs.size();
        long distributed = 0;
        for (int i = 0; i < outs.size(); i++) {
            long add = perContainer + (i == 0 ? remainder : 0);
            outs.get(i).addEnergy(add);
            distributed += add;
        }
        return distributed;
    }

    private List<NotifiableEnergyContainer> findOutputEnergyContainers() {
        List<NotifiableEnergyContainer> out = new ArrayList<>();
        for (IMultiPart part : getParts()) {
            MetaMachine m = part.self();
            if (m instanceof EnergyHatchPartMachine eh) {
                // emitter container = OUTPUT_ENERGY (getOutputVoltage>0 で判定)
                if (eh.energyContainer != null && eh.energyContainer.getOutputVoltage() > 0) {
                    out.add(eh.energyContainer);
                }
            }
        }
        return out;
    }

    /** 入力電力から要求分を取れるだけ消費 (= BH 過消費用、 足りなくても進める) */
    public long tryConsumeEnergyFromContainer(long required) {
        if (energyContainer == null || required <= 0) return 0;
        long stored = energyContainer.getEnergyStored();
        long taken = Math.min(stored, required);
        if (taken > 0) energyContainer.removeEnergy(taken);
        return taken;
    }

    // =========================================================================
    //  Hatch handler 検索 — 内部
    // =========================================================================

    private List<IItemHandlerModifiable> findItemHandlers(IO direction) {
        List<IItemHandlerModifiable> out = new ArrayList<>();
        var byCap = getCapabilitiesProxy().get(direction, ItemRecipeCapability.CAP);
        if (byCap == null) return out;
        for (IRecipeHandler<?> h : byCap) {
            if (h instanceof NotifiableItemStackHandler nh) {
                out.add(nh.storage);
            }
        }
        return out;
    }

    private List<NotifiableFluidTank> findFluidTanks(IO direction) {
        List<NotifiableFluidTank> out = new ArrayList<>();
        var byCap = getCapabilitiesProxy().get(direction, FluidRecipeCapability.CAP);
        if (byCap == null) return out;
        for (IRecipeHandler<?> h : byCap) {
            if (h instanceof NotifiableFluidTank tank) {
                out.add(tank);
            }
        }
        return out;
    }

    // =========================================================================
    //  共通出力 — emitOutputsOnSuccess のデフォルト経路から呼ばれる
    // =========================================================================

    public void emitCommonOutputs(StarForgeTraceData.TraceInfo info) {
        LOGGER.info("[StarForge:{}] emit common outputs: {} items + {} fluids",
                info.trace, info.outputItems.size(), info.outputFluids.size());
        for (ItemStack s : info.outputItems) {
            if (s != null && !s.isEmpty()) outputItem(s.copy());
        }
        for (FluidStack f : info.outputFluids) {
            if (f != null && !f.isEmpty()) outputFluid(f.copy());
        }
    }

    // =========================================================================
    //  Display (= controller block 右クリック GUI の文字表示)
    // =========================================================================

    @Override
    public void addDisplayText(List<Component> textList) {
        super.addDisplayText(textList);
        if (!isFormed()) return;

        // Phase
        ChatFormatting phaseColor = switch (phase) {
            case IDLE -> ChatFormatting.GRAY;
            case BUILD -> ChatFormatting.YELLOW;
            case MATURITY -> ChatFormatting.AQUA;
            case DECAY -> ChatFormatting.LIGHT_PURPLE;
        };
        textList.add(Component.literal("Phase: " + phase.name())
                .withStyle(Style.EMPTY.withColor(phaseColor)));

        // Trace
        if (currentTrace == null) {
            textList.add(Component.literal("Trace: (none — insert star_locus to start)")
                    .withStyle(ChatFormatting.GRAY));
            return;
        }
        textList.add(Component.literal("Trace: " + currentTrace)
                .withStyle(ChatFormatting.GOLD));

        StarForgeTraceData.TraceInfo info = currentInfo();
        if (info == null) return;
        textList.add(Component.literal("Kind: " + info.kind.name())
                .withStyle(ChatFormatting.WHITE));

        // Phase 固有情報
        switch (phase) {
            case BUILD -> {
                long req = info.buildRequiredCount;
                double pct = req > 0 ? (buildProgress * 100.0 / req) : 0.0;
                textList.add(Component.literal(String.format("Build: %,d / %,d  (%.2f%%)",
                        buildProgress, req, pct)));
            }
            case MATURITY -> {
                long matDur = maturityDurationFor(info);
                if (matDur > 0) {
                    double pct = (maturityElapsed * 100.0 / matDur);
                    textList.add(Component.literal(String.format("Maturity: %,d / %,d tick  (%.1f%%)",
                            maturityElapsed, matDur, pct)));
                }
                if (info.kind == StarForgeTraceData.Kind.MATURITY_SUN) {
                    int need = SunKindLogic.SOLAR_REQUIREMENT;
                    ChatFormatting solarColor = (solarPanelCount >= need)
                            ? ChatFormatting.GREEN : ChatFormatting.YELLOW;
                    textList.add(Component.literal(String.format("UV Solar Panels: %d / %d",
                            solarPanelCount, need)).withStyle(solarColor));
                    if (solarPanelCount >= need) {
                        textList.add(Component.literal(String.format("Emitting: %,d EU/t (UHV 32A)",
                                SunKindLogic.SUN_OUTPUT_EUT)).withStyle(ChatFormatting.GREEN));
                    }
                } else if (info.kind == StarForgeTraceData.Kind.MATURITY_BLACK_HOLE) {
                    textList.add(Component.literal(String.format("Emission queue: %d tick remaining",
                            bhEmissionTicks)));
                    if (bhEmissionTicks > 0) {
                        textList.add(Component.literal(String.format("Emitting: %,d EU/t (MAX+1V × 2A)",
                                BlackHoleKindLogic.BH_EMISSION_EUT)).withStyle(ChatFormatting.GREEN));
                    }
                }
            }
            case DECAY -> {
                if (info.kind == StarForgeTraceData.Kind.NORMAL) {
                    long req = info.decayRequiredCount;
                    double pct = req > 0 ? (decayProgress * 100.0 / req) : 0.0;
                    textList.add(Component.literal(String.format("Decay: %,d / %,d  (%.2f%%)",
                            decayProgress, req, pct)));
                } else if (info.kind == StarForgeTraceData.Kind.MATURITY_BLACK_HOLE) {
                    ChatFormatting decayColor = bhDecayPercent <= 10
                            ? ChatFormatting.RED
                            : bhDecayPercent <= 50 ? ChatFormatting.GOLD
                            : ChatFormatting.WHITE;
                    textList.add(Component.literal(String.format("Decay: %.2f%%   Threshold: %d%%",
                            bhDecayPercent, bhSurgeThreshold)).withStyle(decayColor));
                    if (bhSurgeTicksRemaining > 0) {
                        int elapsed = BlackHoleKindLogic.SURGE_TOTAL_TICKS - bhSurgeTicksRemaining;
                        textList.add(Component.literal(String.format(
                                "★ SURGE ACTIVE ★  %d / %d tick  — TURN OFF MACHINE NOW",
                                elapsed, BlackHoleKindLogic.SURGE_TOTAL_TICKS))
                                .withStyle(Style.EMPTY.withColor(ChatFormatting.RED).withBold(true)));
                    } else if (bhSurgeStarted) {
                        textList.add(Component.literal("Surge ended (no power-off detected)")
                                .withStyle(ChatFormatting.GRAY));
                    } else {
                        textList.add(Component.literal("Awaiting surge trigger…")
                                .withStyle(ChatFormatting.GRAY));
                    }
                } else if (info.kind == StarForgeTraceData.Kind.MATURITY_SUN) {
                    textList.add(Component.literal("Decay (instant)").withStyle(ChatFormatting.GRAY));
                }
            }
            default -> {}
        }
    }

    private static long maturityDurationFor(StarForgeTraceData.TraceInfo info) {
        return switch (info.kind) {
            case MATURITY_SUN -> SunKindLogic.MATURITY_DURATION;
            case MATURITY_BLACK_HOLE -> BlackHoleKindLogic.MATURITY_DURATION;
            default -> 0L;
        };
    }
}
