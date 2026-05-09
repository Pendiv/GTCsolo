package DIV.gtcsolo.api.tier;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.recipe.EURecipeCapability;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.api.recipe.content.Content;
import com.gregtechceu.gtceu.api.recipe.modifier.ModifierFunction;
import com.gregtechceu.gtceu.data.recipe.builder.GTRecipeBuilder;
import com.gregtechceu.gtceu.utils.GTUtil;
import com.lowdragmc.lowdraglib.utils.LocalizationUtils;
import net.minecraft.data.recipes.FinishedRecipe;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * 構造tier ↔ レシピ要求tier の連携ヘルパー。
 *
 * 設計:
 * - レシピ側は {@code recipe.data.putInt("requiredTier", GTValues.HV)} のように要求tierを持たせる
 * - data に "requiredTier" が無いレシピは「tier 要求なし」として、構造 tier に関わらず実行可
 * - {@link #tierGate} を {@code recipeModifiers} に組み込めば素のゲート機能のみ提供
 *   (並列やOC強化はマシンごとの内部ロジックで上乗せする方針)
 */
public final class TierRecipeLogic {

    /** レシピデータ側のキー。 */
    public static final String RECIPE_DATA_KEY = "requiredTier";

    private TierRecipeLogic() {}

    /** レシピが要求する電圧tier。data に無ければ -1 (要求なし)。 */
    public static int getRequiredTier(GTRecipe recipe) {
        return recipe.data != null && recipe.data.contains(RECIPE_DATA_KEY)
                ? recipe.data.getInt(RECIPE_DATA_KEY)
                : -1;
    }

    /** マシンtier がレシピ要求tier 以上か。要求なしのレシピは常に true。 */
    public static boolean canRunRecipe(int machineTier, GTRecipe recipe) {
        int required = getRequiredTier(recipe);
        return required < 0 || machineTier >= required;
    }

    /**
     * RecipeModifier 用の純粋なゲート。
     * - {@link TieredMultiblockMachine} 以外のマシンには無効 (IDENTITY を返す)
     * - 要求 tier > 構造 tier なら NULL (= 不可)
     * - それ以外は IDENTITY (= 素通し)
     *
     * 並列/OC を上乗せしたい場合はこれと別の RecipeModifier を併用する。
     */
    public static ModifierFunction tierGate(@Nonnull MetaMachine machine, @Nonnull GTRecipe recipe) {
        if (!(machine instanceof TieredMultiblockMachine tiered)) {
            return ModifierFunction.IDENTITY;
        }
        int required = getRequiredTier(recipe);
        if (required < 0) return ModifierFunction.IDENTITY;
        if (tiered.getStructureTier() < required) return ModifierFunction.NULL;
        return ModifierFunction.IDENTITY;
    }

    /**
     * GTRecipeType の JEI 表示に "Required Tier: %s" 行を追加する。
     * data に "requiredTier" が無いレシピでは行が出ない (空文字列を返す)。
     *
     * <p>この呼び出しは GTRecipeType 自体に効くため、同じ recipe type を共有する
     * 全レシピの JEI 表示に影響する点に注意 (data 無しレシピは空行のため実害なし)。
     */
    public static GTRecipeType addRequiredTierDisplay(GTRecipeType recipeType) {
        return recipeType.addDataInfo(data -> {
            if (data == null || !data.contains(RECIPE_DATA_KEY)) return "";
            int tier = data.getInt(RECIPE_DATA_KEY);
            if (tier < 0 || tier >= GTValues.VNF.length) return "";
            // VNF は GT 純正の tier 色 (LV=GRAY, MV=AQUA, HV=GOLD, ...) が前置されている
            return LocalizationUtils.format("gtcsolo.recipe.required_tier", GTValues.VNF[tier]);
        });
    }

    /**
     * onRecipeBuild 用フック: ビルダー側に requiredTier がまだ無い場合、
     * EUt 入力電圧から自動で {@link #RECIPE_DATA_KEY} を data に詰める。
     * 既に明示指定されているレシピは尊重して上書きしない。
     *
     * <p>使用例: {@code MY_RECIPE_TYPE.onRecipeBuild(TierRecipeLogic.stampRequiredTierFromEUt());}
     */
    public static BiConsumer<GTRecipeBuilder, Consumer<FinishedRecipe>> stampRequiredTierFromEUt() {
        return (builder, consumer) -> {
            if (builder == null || builder.data == null) return;
            if (builder.data.contains(RECIPE_DATA_KEY)) return;
            long eut = extractRecipeEUt(builder);
            if (eut <= 0) return;
            int tier = GTUtil.getTierByVoltage(eut);
            if (tier >= 0 && tier < GTValues.VN.length) {
                builder.data.putInt(RECIPE_DATA_KEY, tier);
            }
        };
    }

    /** GTRecipeBuilder の tickInput から EU/t 入力電圧を取り出す。無ければ 0。 */
    private static long extractRecipeEUt(GTRecipeBuilder builder) {
        List<Content> contents = builder.tickInput.get(EURecipeCapability.CAP);
        if (contents == null || contents.isEmpty()) return 0;
        Object obj = contents.get(0).content;
        return obj instanceof Long l ? l : 0L;
    }
}
