package DIV.gtcsolo.data.recipe;

import DIV.gtcsolo.item.AbstractLocusItem;
import DIV.gtcsolo.registry.ModItems;
import DIV.gtcsolo.registry.ModRecipeTypes;
import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.recipe.ingredient.NBTIngredient;
import com.gregtechceu.gtceu.data.recipe.builder.GTRecipeBuilder;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.function.Consumer;

/**
 * locus_simulation_builder の自動生成レシピ (8 軌跡)。
 *
 * <ul>
 *   <li>入力: 空 star_locus (NBT nil) — 必ず 1 個</li>
 *   <li>触媒 (notConsumable): 前 tier 軌跡の decaying_star_locus — 起点軌跡 (褐色矮星) は不要</li>
 *   <li>出力: Trace NBT 付き star_locus</li>
 *   <li>EUt: 太陽未満 ZPM / 太陽以上 UV、duration: 全軌跡 72000 tick</li>
 * </ul>
 *
 * tier 連鎖 (各軌跡は 1 つ前の decaying を要求):
 * brown_dwarf → koi74 → r_andromedae → hd101065 → cemp_r → sun → neutron_star → black_hole
 */
public final class LocusSimulationBuilderRecipes {
    private LocusSimulationBuilderRecipes() {}

    private static final int DURATION = 72000;

    public static void register(Consumer<FinishedRecipe> writer) {
        // 8 軌跡それぞれに circuit 1〜8 を振って GT recipe lookup tree 上で distinct 化。
        // 同一入力 (= empty star_locus) 8 件が衝突して JEI 表示が 1 件にまとまる仮説を検証。
        // 起点 (褐色矮星): decaying 不要
        register(writer, AbstractLocusItem.BROWN_DWARF,  null,                            GTValues.ZPM, 1);
        // 連鎖 (太陽未満 = ZPM)
        register(writer, AbstractLocusItem.KOI74,        AbstractLocusItem.BROWN_DWARF,   GTValues.ZPM, 2);
        register(writer, AbstractLocusItem.R_ANDROMEDAE, AbstractLocusItem.KOI74,         GTValues.ZPM, 3);
        register(writer, AbstractLocusItem.HD101065,     AbstractLocusItem.R_ANDROMEDAE,  GTValues.ZPM, 4);
        register(writer, AbstractLocusItem.CEMP_R,       AbstractLocusItem.HD101065,      GTValues.ZPM, 5);
        // 太陽以上 = UV
        register(writer, AbstractLocusItem.SUN,          AbstractLocusItem.CEMP_R,        GTValues.UV,  6);
        register(writer, AbstractLocusItem.NEUTRON_STAR, AbstractLocusItem.SUN,           GTValues.UV,  7);
        register(writer, AbstractLocusItem.BLACK_HOLE,   AbstractLocusItem.NEUTRON_STAR,  GTValues.UV,  8);
    }

    /**
     * @param outputTrace    出力する star_locus の Trace
     * @param decayingTrace  触媒となる decaying_star_locus の Trace (起点軌跡なら null)
     * @param tier           GTValues の tier index (ZPM=7 / UV=8)
     * @param circuit        programmed circuit configuration (1〜8、 各軌跡で distinct)
     */
    private static void register(Consumer<FinishedRecipe> writer,
                                  String outputTrace,
                                  String decayingTrace,
                                  int tier,
                                  int circuit) {
        GTRecipeBuilder builder = ModRecipeTypes.LOCUS_SIMULATION_BUILDER
                .recipeBuilder("autogen_locus_" + outputTrace)
                .inputItems(emptyLocusIngredient())
                .outputItems(AbstractLocusItem.of(ModItems.STAR_LOCUS.get(), outputTrace))
                .duration(DURATION)
                .EUt(GTValues.V[tier])
                .circuitMeta(circuit);
        if (decayingTrace != null) {
            builder.notConsumable(decayingTraceIngredient(decayingTrace));
        }
        builder.save(writer);
    }

    /** Trace 未設定の star_locus のみマッチ。 LocusIngredient で tag=null/{} を吸収 */
    private static Ingredient emptyLocusIngredient() {
        return DIV.gtcsolo.api.ingredient.LocusIngredient.empty(ModItems.STAR_LOCUS.get());
    }

    /** 指定 Trace の decaying_star_locus のみマッチ */
    private static Ingredient decayingTraceIngredient(String trace) {
        return DIV.gtcsolo.api.ingredient.LocusIngredient.ofTrace(
                ModItems.DECAYING_STAR_LOCUS.get(), trace);
    }
}
