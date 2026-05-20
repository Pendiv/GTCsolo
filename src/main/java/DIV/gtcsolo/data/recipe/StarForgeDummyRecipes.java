package DIV.gtcsolo.data.recipe;

import DIV.gtcsolo.item.AbstractLocusItem;
import DIV.gtcsolo.machine.starforge.PhaseProgressionTable;
import DIV.gtcsolo.machine.starforge.StarForgeTraceData;
import DIV.gtcsolo.registry.ModItems;
import DIV.gtcsolo.registry.ModRecipeTypes;
import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.recipe.ingredient.NBTIngredient;
import com.gregtechceu.gtceu.data.recipe.builder.GTRecipeBuilder;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.fluids.FluidStack;

import java.util.function.Consumer;

/**
 * StarForge 用 GT 標準 JEI ダミーレシピ。 全 8 軌跡分を自動生成する。
 *
 * <p>仕様 (docs/StarForge_spec.md §9.5.2、 ver.0.5 §4.4):
 * <ul>
 *   <li>真: 出力アイテム/液体 = TraceInfo.outputItems / outputFluids</li>
 *   <li>偽: 入力アイテム = 構築フェイズ進捗テーブルの全 item を count=1 で並べる (= 種類提示のみ)</li>
 *   <li>中間: duration = 通常型は decayRequiredCount、 成熟型は maturityDurationTicks (= AVG)</li>
 *   <li>中間: EUt = trace ごとの代表値 (AVG、 仮値、 確定したらここで更新)</li>
 *   <li>触媒 (notConsumable): trace 付き star_locus = レシピ識別用 (実消費は machine 側 logic 担当)</li>
 *   <li>"朽ち果てた星の軌跡" は JEI に出さない (= 実機運行時のみ出力、 machine logic で処理)</li>
 * </ul>
 */
public final class StarForgeDummyRecipes {
    private StarForgeDummyRecipes() {}

    public static void register(Consumer<FinishedRecipe> writer) {
        for (StarForgeTraceData.TraceInfo info : StarForgeTraceData.all()) {
            buildDummyRecipe(info, writer);
        }
    }

    private static void buildDummyRecipe(StarForgeTraceData.TraceInfo info, Consumer<FinishedRecipe> writer) {
        GTRecipeBuilder b = ModRecipeTypes.STARFORGE
                .recipeBuilder("dummy_starforge_" + info.trace);

        // 触媒: trace 付き star_locus (notConsumable = レシピ識別子扱い、 JEI で catalyst として表示)
        b.notConsumable(tracedLocusIngredient(info.trace));

        // 入力: 構築フェイズ進捗テーブルの全 item を count=1 で並べる (= 偽: 種類提示のみ)
        if (info.buildPhaseTable != null) {
            for (Item item : info.buildPhaseTable.getDefaultItems()) {
                if (item == null) continue;
                b.inputItems(new ItemStack(item, 1));
            }
            for (PhaseProgressionTable.EffectiveEntry e : info.buildPhaseTable.getEffectiveEntries()) {
                if (e.displayStack != null && !e.displayStack.isEmpty()) {
                    b.inputItems(e.displayStack.copy());
                }
            }
        }

        // 出力: 真の値 (= TraceInfo の outputItems / outputFluids)
        for (ItemStack out : info.outputItems) {
            if (out != null && !out.isEmpty()) b.outputItems(out.copy());
        }
        for (FluidStack out : info.outputFluids) {
            if (out != null && !out.isEmpty()) b.outputFluids(out.copy());
        }

        // duration: 通常型 = 崩壊要求 count を 1tick/item 換算の下限、 成熟型 = maturity 値
        long duration = info.kind == StarForgeTraceData.Kind.NORMAL
                ? Math.max(info.decayRequiredCount, 1L)
                : Math.max(info.maturityDurationTicks, 1L);
        // GT recipe builder duration は int。 上限超過は MAX_VALUE で clamp
        b.duration((int) Math.min(duration, Integer.MAX_VALUE));

        // EUt: trace 別 AVG (仮値、 確定したら更新)
        long eut = computeAverageEUt(info);
        b.EUt(eut);

        b.save(writer);
    }

    /**
     * trace ごとの AVG 電圧。 ver.0.5 時点は仮値。
     *  - 通常型: VA[LV] (= ほぼ消費しない、 アイテム搬入レートが律速)
     *  - 太陽: maturityEUt が設定済ならそれ、 そうでなければ VA[ZPM]
     *  - BH: maturityEUt が設定済ならそれ、 そうでなければ VA[MAX]
     */
    private static long computeAverageEUt(StarForgeTraceData.TraceInfo info) {
        if (info.maturityEUt > 0) return info.maturityEUt;
        return switch (info.kind) {
            case NORMAL -> GTValues.VA[GTValues.LV];
            case MATURITY_SUN -> GTValues.VA[GTValues.ZPM];
            case MATURITY_BLACK_HOLE -> GTValues.VA[GTValues.MAX];
        };
    }

    private static Ingredient tracedLocusIngredient(String trace) {
        return NBTIngredient.createNBTIngredient(
                AbstractLocusItem.of(ModItems.STAR_LOCUS.get(), trace)
        );
    }
}
