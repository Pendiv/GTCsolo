package DIV.gtcsolo.machine;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.TickableSubscription;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiPart;
import com.gregtechceu.gtceu.api.machine.multiblock.CoilWorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.RecipeHelper;
import com.gregtechceu.gtceu.api.recipe.content.ContentModifier;
import com.gregtechceu.gtceu.api.recipe.modifier.ModifierFunction;
import com.gregtechceu.gtceu.api.recipe.modifier.ParallelLogic;
import com.gregtechceu.gtceu.common.machine.multiblock.part.EnergyHatchPartMachine;
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Random;

/**
 * SpaceForge コントローラー。
 *
 * 状態機械:
 *   PREPARING → (温度上限到達) → OPERATING → (損傷100%) → REPAIRING → (損傷0%) → STOPPED
 *
 * レシピ稼働は OPERATING 状態のみ。
 * EUは毎tick 全エネルギーハッチから抜き出して温度に変換。
 * SEHatch (main hatch) の Tier が OC境界と通常エネルギーハッチ Tier 制約を決定。
 */
public class SpaceforgeMachine extends CoilWorkableElectricMultiblockMachine {

    public static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            SpaceforgeMachine.class, CoilWorkableElectricMultiblockMachine.MANAGED_FIELD_HOLDER);

    public static final long MAX_TEMPERATURE = 100_000_000L;
    public static final int COIL_RESONANCE_THRESHOLD = 4000;  // このコイル温度未満だと損傷ブースト
    public static final int HEAT_CONVERSION_RATIO = 10;        // 10 EU = 1 F (ベース)
    public static final double MAX_EFFICIENCY_BONUS = 3.0;     // +300% (400%=4x)

    public enum State {
        PREPARING,   // 稼働準備（温度上昇中）
        OPERATING,   // レシピ稼働可能
        REPAIRING,   // 修復中
        STOPPED      // 稼働停止（ユーザー再起動待ち）
    }

    @Persisted @DescSynced private long temperature = 0;
    @Persisted @DescSynced private double damage = 0.0;
    @Persisted @DescSynced private State state = State.PREPARING;

    // レシピ稼働状態の前tick値 (稼働開始検知用、保存不要)
    private boolean wasWorking = false;

    // 構造検証エラー（Tier不一致など）
    private boolean structureValid = true;
    private Component structureError = null;

    // SEHatch 参照（構造形成時にキャッシュ）
    private int mainHatchTier = -1;

    private TickableSubscription tickSub;
    private final Random random = new Random();

    public SpaceforgeMachine(IMachineBlockEntity holder, Object... args) {
        super(holder);
    }

    /** コイル温度に基づくEU→熱変換の効率倍率 (1.0～4.0) */
    public double getCoilEfficiency() {
        int coilTemp = getCoilType().getCoilTemperature();
        if (coilTemp <= COIL_RESONANCE_THRESHOLD) return 1.0;
        int over = coilTemp - COIL_RESONANCE_THRESHOLD;
        double bonus = Math.floor(over / 100.0) * 0.05;
        return 1.0 + Math.min(bonus, MAX_EFFICIENCY_BONUS);
    }

    /** コイルが共振熱に耐えられるか */
    public boolean coilCanHandleResonance() {
        return getCoilType().getCoilTemperature() >= COIL_RESONANCE_THRESHOLD;
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    public long getTemperature() { return temperature; }
    public double getDamage() { return damage; }
    public State getState() { return state; }
    public int getMainHatchTier() { return mainHatchTier; }

    // =========================================================================
    //  構造形成 / 検証
    // =========================================================================

    @Override
    public void onStructureFormed() {
        super.onStructureFormed();
        validateHatches();
    }

    @Override
    public void onStructureInvalid() {
        super.onStructureInvalid();
        mainHatchTier = -1;
        structureValid = true;
        structureError = null;
        if (tickSub != null) { unsubscribe(tickSub); tickSub = null; }
    }

    private void validateHatches() {
        SpaceforgeEnergyHatchMachine main = null;
        List<EnergyHatchPartMachine> normals = new java.util.ArrayList<>();

        for (IMultiPart part : getParts()) {
            MetaMachine m = part.self();
            if (m instanceof SpaceforgeEnergyHatchMachine sh) {
                main = sh;
            } else if (m instanceof EnergyHatchPartMachine eh) {
                normals.add(eh);
            }
        }

        if (main == null) {
            structureValid = false;
            structureError = Component.translatable("gtcsolo.machine.spaceforge.error.no_sehatch");
            return;
        }

        mainHatchTier = main.getTier();
        int requiredNormalTier = mainHatchTier - 3;

        for (EnergyHatchPartMachine eh : normals) {
            if (eh.getTier() != requiredNormalTier) {
                structureValid = false;
                structureError = Component.translatable("gtcsolo.machine.spaceforge.error.wrong_tier",
                        GTValues.VNF[eh.getTier()], GTValues.VNF[requiredNormalTier]);
                return;
            }
        }

        structureValid = true;
        structureError = null;

        if (tickSub == null) {
            tickSub = subscribeServerTick(this::onServerTick);
        }
    }

    // =========================================================================
    //  サーバーティック
    // =========================================================================

    private long tickCount = 0;

    private void onServerTick() {
        if (!isFormed() || !structureValid) return;
        tickCount++;

        // レシピ稼働の立ち上がり検知 (IDLE/WAITING → WORKING への遷移)
        // 遷移時のみ損傷度ロール（modifier内だと毎マッチ試行で走るため）
        if (state == State.OPERATING && getRecipeLogic() != null) {
            boolean nowWorking = getRecipeLogic().isWorking();
            if (nowWorking && !wasWorking) {
                // レシピが実際に稼働開始した
                if (random.nextFloat() < 0.5f) {
                    double delta = coilCanHandleResonance() ? 0.01 : 0.01 * 20;
                    damage += delta;
                    if (damage >= 1.0) {
                        damage = 1.0;
                        state = State.REPAIRING;
                        temperature = 0;
                    }
                }
            }
            wasWorking = nowWorking;
        } else {
            wasWorking = false;
        }

        switch (state) {
            case PREPARING -> {
                consumeEnergyToHeat();
                if (temperature >= MAX_TEMPERATURE) {
                    state = State.OPERATING;
                }
            }
            case OPERATING -> {
                // 温度維持用にも吸い続ける（上限に常に張り付く）
                consumeEnergyToHeat();
                // 毎秒 (20tick) 50% で +0.3% 損傷 (コイルが共振熱に耐えられなければ×20)
                if (tickCount % 20 == 0 && random.nextFloat() < 0.5f) {
                    double delta = coilCanHandleResonance() ? 0.003 : 0.003 * 20;
                    damage += delta;
                    if (damage >= 1.0) {
                        damage = 1.0;
                        state = State.REPAIRING;
                        temperature = 0; // 修復状態へ移行時、温度を失う
                    }
                }
            }
            case REPAIRING -> {
                // エネルギー消費停止、毎秒 -1%
                if (tickCount % 20 == 0) {
                    damage -= 0.01;
                    if (damage <= 0.0) {
                        damage = 0.0;
                        state = State.STOPPED;
                        setWorkingEnabled(false);
                        temperature = 0;
                    }
                }
            }
            case STOPPED -> {
                // ユーザー再起動待ち (setWorkingEnabled(true) で PREPARING に戻る)
                if (isWorkingEnabled()) {
                    state = State.PREPARING;
                }
            }
        }
    }

    /**
     * 全エネルギーハッチから EU を抜き出し、温度に変換する。
     * ベース: 10 EU = 1 F。コイル温度が4000超の場合、100毎に+5%効率ボーナス (最大 400%)。
     */
    private void consumeEnergyToHeat() {
        if (temperature >= MAX_TEMPERATURE) return;
        double efficiency = getCoilEfficiency(); // 1.0 ～ 4.0
        if (energyContainer == null) return;
        long stored = energyContainer.getEnergyStored();
        if (stored <= 0) return;

        long space = MAX_TEMPERATURE - temperature;                              // 残り熱容量 (F)
        // EU × efficiency / 10 = F
        long heatGain = (long) (stored * efficiency / HEAT_CONVERSION_RATIO);
        long actualHeatGain = Math.min(heatGain, space);
        if (actualHeatGain <= 0) return;

        // 実際に消費するEU: actualHeatGain × 10 / efficiency
        long euToTake = (long) (actualHeatGain * HEAT_CONVERSION_RATIO / efficiency);
        euToTake = Math.min(euToTake, stored);

        energyContainer.removeEnergy(euToTake);
        temperature += actualHeatGain;
    }

    // =========================================================================
    //  レシピ修飾 (OC + 状態チェック)
    // =========================================================================

    public static ModifierFunction spaceforgeOverclock(MetaMachine machine, @Nonnull GTRecipe recipe) {
        if (!(machine instanceof SpaceforgeMachine sf)) return ModifierFunction.IDENTITY;

        // 状態チェック: OPERATING 以外はレシピ拒否
        if (sf.state != State.OPERATING || !sf.structureValid) {
            return ModifierFunction.NULL;
        }

        int recipeTier = RecipeHelper.getRecipeEUtTier(recipe);
        int machineTier = sf.getTier();
        int mainTier = sf.mainHatchTier;
        if (mainTier < 0) return ModifierFunction.NULL;

        // 注: レシピ起動ごとの損傷度ロールは onServerTick() で稼働開始検知時に行う
        // (modifier はマッチ試行毎に呼ばれるため、ここでロールすると副作用が多発する)

        // OC計算
        int totalOc = Math.max(0, machineTier - recipeTier);
        int normalOc = Math.max(0, Math.min(mainTier - recipeTier, totalOc));
        int imperfectOc = totalOc - normalOc;

        double eutMultiplier = Math.pow(4.0, totalOc);
        double durationMultiplier = Math.pow(0.5, normalOc) * Math.pow(0.75, imperfectOc);

        // 並列数計算: SEHatch tier が ZPM から上がるごとに +floor(3.6^n)
        int maxParallels = computeParallels(mainTier);

        // EU予算で並列をキャップ
        long baseEut = RecipeHelper.getInputEUt(recipe);
        long availableEut = sf.energyContainer != null
                ? sf.energyContainer.getInputVoltage() * sf.energyContainer.getInputAmperage() : 0;
        long perParallelEut = (long) (baseEut * eutMultiplier);
        int maxByEu = perParallelEut > 0 ? (int) Math.max(1, availableEut / perParallelEut) : maxParallels;
        int eutCappedParallels = Math.min(maxParallels, maxByEu);

        // 入力/出力スペースでさらにキャップ
        int parallels = eutCappedParallels;
        if (eutCappedParallels > 1) {
            parallels = ParallelLogic.getParallelAmount(sf, recipe, eutCappedParallels);
            if (parallels <= 0) parallels = 1;
        }

        ModifierFunction.FunctionBuilder builder = ModifierFunction.builder()
                .eutMultiplier(eutMultiplier * parallels)
                .durationMultiplier(durationMultiplier)
                .parallels(parallels);
        if (parallels > 1) {
            builder.modifyAllContents(ContentModifier.multiplier(parallels));
        }
        return builder.build();
    }

    /** SEHatch Tier から並列数を計算 (ZPM基準、floor(3.6^n), UV以降) */
    public static int computeParallels(int seHatchTier) {
        int n = Math.max(0, seHatchTier - GTValues.ZPM);
        return (int) Math.max(1, Math.floor(Math.pow(3.6, n)));
    }

    // =========================================================================
    //  UI 表示
    // =========================================================================

    @Override
    public void addDisplayText(List<Component> textList) {
        super.addDisplayText(textList);
        if (!isFormed()) return;

        if (!structureValid && structureError != null) {
            textList.add(structureError.copy().withStyle(Style.EMPTY.withColor(ChatFormatting.RED)));
            return;
        }

        // 状態表示
        ChatFormatting stateColor = switch (state) {
            case PREPARING -> ChatFormatting.YELLOW;
            case OPERATING -> ChatFormatting.GREEN;
            case REPAIRING -> ChatFormatting.RED;
            case STOPPED -> ChatFormatting.GRAY;
        };
        textList.add(Component.translatable("gtcsolo.machine.spaceforge.state")
                .append(Component.translatable("gtcsolo.machine.spaceforge.state." + state.name().toLowerCase()))
                .setStyle(Style.EMPTY.withColor(stateColor)));

        // 温度表示
        textList.add(Component.translatable("gtcsolo.machine.spaceforge.temperature",
                String.format("%,d", temperature), String.format("%,d", MAX_TEMPERATURE)));

        // 損傷度表示
        ChatFormatting damageColor = damage >= 0.9 ? ChatFormatting.RED
                : damage >= 0.5 ? ChatFormatting.GOLD
                : ChatFormatting.WHITE;
        textList.add(Component.translatable("gtcsolo.machine.spaceforge.damage",
                String.format("%.1f", damage * 100)).withStyle(damageColor));

        // SEHatch Tier + 並列数
        if (mainHatchTier >= 0) {
            textList.add(Component.translatable("gtcsolo.machine.spaceforge.sehatch_tier",
                    GTValues.VNF[mainHatchTier]));
            textList.add(Component.translatable("gtcsolo.machine.spaceforge.parallels",
                    computeParallels(mainHatchTier)));
        }

        // レシピ稼働状態
        if (getRecipeLogic() != null) {
            boolean working = getRecipeLogic().isWorking();
            boolean waiting = getRecipeLogic().isWaiting();
            ChatFormatting recipeColor = working ? ChatFormatting.GREEN
                    : waiting ? ChatFormatting.YELLOW
                    : ChatFormatting.GRAY;
            String keySuffix = working ? "working" : waiting ? "waiting" : "idle";
            textList.add(Component.translatable("gtcsolo.machine.spaceforge.recipe_state")
                    .append(Component.translatable("gtcsolo.machine.spaceforge.recipe_state." + keySuffix))
                    .setStyle(Style.EMPTY.withColor(recipeColor)));
        }

        // コイル情報
        int coilTemp = getCoilType().getCoilTemperature();
        double eff = getCoilEfficiency();
        boolean canHandle = coilCanHandleResonance();
        ChatFormatting coilColor = canHandle ? ChatFormatting.AQUA : ChatFormatting.RED;
        textList.add(Component.translatable("gtcsolo.machine.spaceforge.coil",
                coilTemp, String.format("%.0f", eff * 100)).withStyle(coilColor));
        if (!canHandle) {
            textList.add(Component.translatable("gtcsolo.machine.spaceforge.coil_warning")
                    .withStyle(ChatFormatting.RED));
        }
    }
}