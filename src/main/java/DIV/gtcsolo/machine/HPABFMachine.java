package DIV.gtcsolo.machine;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiPart;
import com.gregtechceu.gtceu.api.machine.multiblock.CoilWorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.pattern.MultiblockState;
import com.gregtechceu.gtceu.api.pattern.TraceabilityPredicate;
import com.gregtechceu.gtceu.api.pattern.error.PatternStringError;
import com.gregtechceu.gtceu.api.pattern.predicates.SimplePredicate;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.RecipeHelper;
import com.gregtechceu.gtceu.api.recipe.modifier.ModifierFunction;
import com.gregtechceu.gtceu.api.recipe.modifier.RecipeModifier;

import javax.annotation.Nonnull;

public class HPABFMachine extends CoilWorkableElectricMultiblockMachine {

    // 差別化: 適切な熱処理と堅牢な構造でコイル熱上限を150%に強化
    private static final double COIL_MAX_TEMP_MULTIPLIER = 1.5;
    // OC1段ごとの温度上昇: 都度 (T + 100) × 1.3 を繰り返す(累積加算+累乗)
    private static final double HEAT_RISE_PER_OC = 1.3;
    private static final int HEAT_ADDITIVE_PER_OC = 100;
    // 昇格閾値: 要求温度より1000Kの超過ごとに不完全OC→完璧OC
    private static final int PROMOTION_STEP = 1000;
    // 電力抵抗による劣化不完全OC
    private static final double IMPERFECT_OC_DURATION = 0.65;
    // 完璧OC (GT標準)
    private static final double PERFECT_OC_DURATION = 0.25;
    // 温度ボーナス発動閾値(要求温度の倍)
    private static final double TEMP_BONUS_THRESHOLD = 2.0;
    private static final double TEMP_BONUS_DURATION = 0.8;
    // EUt OC倍率 (GT標準)
    private static final double OC_EUT_FACTOR = 4.0;
    // EBF標準コイル割引
    private static final int COIL_EUT_DISCOUNT_STEP = 900;
    private static final double COIL_EUT_DISCOUNT = 0.95;

    public HPABFMachine(IMachineBlockEntity holder) {
        super(holder);
    }

    /** コイル熱上限(150%ブースト後) */
    public int getBoostedCoilMaxTemp() {
        int baseCoilTemp = getCoilType().getCoilTemperature();
        int tierBonus = 100 * Math.max(0, getTier() - GTValues.MV);
        return (int) (COIL_MAX_TEMP_MULTIPLIER * (baseCoilTemp + tierBonus));
    }

    /** OC段数n段時の実効温度: 都度(T + 100) × 1.3 を累積適用、コイル上限でキャップ */
    public int computeActualTemp(int recipeTemp, int ocLevels) {
        double t = recipeTemp;
        for (int i = 0; i < ocLevels; i++) {
            t = (t + HEAT_ADDITIVE_PER_OC) * HEAT_RISE_PER_OC;
        }
        return Math.min(getBoostedCoilMaxTemp(), (int) t);
    }

    public static ModifierFunction hpabfOverclock(@Nonnull MetaMachine machine, @Nonnull GTRecipe recipe) {
        if (!(machine instanceof HPABFMachine hpabf)) {
            return RecipeModifier.nullWrongType(HPABFMachine.class, machine);
        }

        // 温度要求チェック(EBF標準)
        if (!recipe.data.contains("ebf_temp")) {
            return ModifierFunction.NULL;
        }
        int recipeTemp = recipe.data.getInt("ebf_temp");
        int coilMaxTemp = hpabf.getBoostedCoilMaxTemp();
        if (recipeTemp > coilMaxTemp) {
            return ModifierFunction.NULL;
        }

        int recipeTier = RecipeHelper.getRecipeEUtTier(recipe);
        int machineTier = hpabf.getTier();
        if (recipeTier > machineTier) {
            return ModifierFunction.NULL;
        }

        // OC段数 = マシンtier - レシピtier
        int totalOC = Math.max(0, machineTier - recipeTier);

        // 実効温度: recipeTemp × 1.15^OC、コイル上限でキャップ
        int actualTemp = hpabf.computeActualTemp(recipeTemp, totalOC);
        int excess = Math.max(0, actualTemp - recipeTemp);

        // 昇格: 1000Kごとに不完全OC→完璧OC(OC段数が上限)
        int promoted = Math.min(totalOC, excess / PROMOTION_STEP);
        int remainingImperfect = totalOC - promoted;

        // Duration倍率
        double durationMult = Math.pow(PERFECT_OC_DURATION, promoted)
                            * Math.pow(IMPERFECT_OC_DURATION, remainingImperfect);

        // 温度ボーナス(OC別枠): T_actual >= 2×T_req
        if (actualTemp >= recipeTemp * TEMP_BONUS_THRESHOLD) {
            durationMult *= TEMP_BONUS_DURATION;
        }

        // EUt倍率: 4^OC × コイル割引(900Kごと0.95x)
        int discountAmount = excess / COIL_EUT_DISCOUNT_STEP;
        double coilDiscount = (recipeTemp < COIL_EUT_DISCOUNT_STEP || discountAmount < 1)
                ? 1.0
                : Math.min(1.0, Math.pow(COIL_EUT_DISCOUNT, discountAmount));
        double eutMult = Math.pow(OC_EUT_FACTOR, totalOC) * coilDiscount;

        return ModifierFunction.builder()
                .eutMultiplier(eutMult)
                .durationMultiplier(durationMult)
                .addOCs(totalOC)
                .build();
    }

    // =========================================================================
    //  排他ハッチPredicate — 他マルチブロックとのパート共有を拒否する
    //  ・ IMultiPartのみチェック対象。平板ブロックは素通り(影響なし)
    //  ・ canShared()の戻り値は変更しない(他MBの挙動に干渉しない)
    //  ・ HPABFの構造検証時のみ走る
    // =========================================================================

    /** TraceabilityPredicateをラップし、全SimplePredicateに排他チェックを追加 */
    public static TraceabilityPredicate exclusive(TraceabilityPredicate base) {
        TraceabilityPredicate result = new TraceabilityPredicate();
        for (SimplePredicate sp : base.common) {
            result.common.add(wrapWithExclusive(sp));
        }
        for (SimplePredicate sp : base.limited) {
            result.limited.add(wrapWithExclusive(sp));
        }
        result.isController = base.isController;
        return result;
    }

    private static SimplePredicate wrapWithExclusive(SimplePredicate original) {
        SimplePredicate copy = new SimplePredicate(original.type,
                state -> original.predicate.test(state) && notInOtherMB(state),
                original.candidates);
        copy.toolTips = original.toolTips;
        copy.minCount = original.minCount;
        copy.maxCount = original.maxCount;
        copy.minLayerCount = original.minLayerCount;
        copy.maxLayerCount = original.maxLayerCount;
        copy.previewCount = original.previewCount;
        copy.disableRenderFormed = original.disableRenderFormed;
        copy.io = original.io;
        copy.slotName = original.slotName;
        copy.nbtParser = original.nbtParser;
        return copy;
    }

    private static boolean notInOtherMB(MultiblockState state) {
        if (state.getTileEntity() instanceof IMachineBlockEntity mbe
                && mbe.getMetaMachine() instanceof IMultiPart part) {
            for (IMultiController c : part.getControllers()) {
                if (!c.self().getPos().equals(state.controllerPos)) {
                    state.setError(new PatternStringError("gtcsolo.multiblock.error.exclusive_parts"));
                    return false;
                }
            }
        }
        return true;
    }
}