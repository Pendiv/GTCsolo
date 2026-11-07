package DIV.gtcsolo.integration.jei.starforge;

import DIV.gtcsolo.integration.jei.GtcSoloJeiPlugin;
import DIV.gtcsolo.item.AbstractLocusItem;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.recipe.IFocus;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * star_locus の右クリックで JEI を開くハンドラ (client-only)。
 * 呼び出し側で Dist.CLIENT 確認すること (このクラスは client side でしかロードしない)。
 */
public final class StarLocusJeiHandler {
    private StarLocusJeiHandler() {}

    public static void openJei(ItemStack stack) {
        IJeiRuntime rt = GtcSoloJeiPlugin.getRuntimeOrNull();
        if (rt == null) return;

        String trace = AbstractLocusItem.getTrace(stack);
        if (trace == null) {
            // 空 star_locus → カテゴリ全 8 ページ表示
            rt.getRecipesGui().showTypes(List.of(StarForgeInfoCategory.RECIPE_TYPE));
            return;
        }

        // Trace 付き → 該当 Trace の star_locus に focus してジャンプ
        // (Category.setRecipe で CATALYST スロットに Trace 付き locus を入れてあるので focus で絞れる)
        ItemStack tracedLocus = AbstractLocusItem.of(stack.getItem(), trace);
        IFocus<ItemStack> focus = rt.getJeiHelpers().getFocusFactory()
                .createFocus(RecipeIngredientRole.CATALYST, VanillaTypes.ITEM_STACK, tracedLocus);
        rt.getRecipesGui().show(focus);
    }
}
