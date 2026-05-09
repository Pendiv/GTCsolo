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
        // 起点 (褐色矮星): decaying 不要
        register(writer, AbstractLocusItem.BROWN_DWARF,  null,                            GTValues.ZPM);
        // 連鎖 (太陽未満 = ZPM)
        register(writer, AbstractLocusItem.KOI74,        AbstractLocusItem.BROWN_DWARF,   GTValues.ZPM);
        register(writer, AbstractLocusItem.R_ANDROMEDAE, AbstractLocusItem.KOI74,         GTValues.ZPM);
        register(writer, AbstractLocusItem.HD101065,     AbstractLocusItem.R_ANDROMEDAE,  GTValues.ZPM);
        register(writer, AbstractLocusItem.CEMP_R,       AbstractLocusItem.HD101065,      GTValues.ZPM);
        // 太陽以上 = UV
        register(writer, AbstractLocusItem.SUN,          AbstractLocusItem.CEMP_R,        GTValues.UV);
        register(writer, AbstractLocusItem.NEUTRON_STAR, AbstractLocusItem.SUN,           GTValues.UV);
        register(writer, AbstractLocusItem.BLACK_HOLE,   AbstractLocusItem.NEUTRON_STAR,  GTValues.UV);
    }

    /**
     * @param outputTrace    出力する star_locus の Trace
     * @param decayingTrace  触媒となる decaying_star_locus の Trace (起点軌跡なら null)
     * @param tier           GTValues の tier index (ZPM=7 / UV=8)
     */
    private static void register(Consumer<FinishedRecipe> writer,
                                  String outputTrace,
                                  String decayingTrace,
                                  int tier) {
        GTRecipeBuilder builder = ModRecipeTypes.LOCUS_SIMULATION_BUILDER
                .recipeBuilder("autogen_locus_" + outputTrace)
                .inputItems(emptyLocusIngredient())
                .outputItems(AbstractLocusItem.of(ModItems.STAR_LOCUS.get(), outputTrace))
                .duration(DURATION)
                .EUt(GTValues.V[tier]);
        if (decayingTrace != null) {
            builder.notConsumable(decayingTraceIngredient(decayingTrace));
        }
        builder.save(writer);
    }

    /** NBT 完全に nil の star_locus を要求する Ingredient (Trace 付きはマッチしない) */
    private static Ingredient emptyLocusIngredient() {
        return NBTIngredient.createNBTIngredient(new ItemStack(ModItems.STAR_LOCUS.get()));
    }

    /** Trace 一致の decaying_star_locus を要求する Ingredient */
    private static Ingredient decayingTraceIngredient(String trace) {
        return NBTIngredient.createNBTIngredient(
                AbstractLocusItem.of(ModItems.DECAYING_STAR_LOCUS.get(), trace)
        );
    }
}
