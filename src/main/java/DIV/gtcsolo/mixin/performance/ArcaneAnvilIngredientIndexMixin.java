package DIV.gtcsolo.mixin.performance;

import DIV.gtcsolo.integration.jei.performance.ArcaneAnvilIngredientIndex;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.runtime.IIngredientManager;
import mezz.jei.library.ingredients.IIngredientSupplier;
import mezz.jei.library.util.IngredientSupplierHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "mezz.jei.library.recipes.RecipeManagerInternal", remap = false)
public abstract class ArcaneAnvilIngredientIndexMixin {
    @Redirect(method = "addRecipe(Lmezz/jei/api/recipe/category/IRecipeCategory;Ljava/lang/Object;Ljava/util/Set;)Z",
            at = @At(value = "INVOKE", target =
            "Lmezz/jei/library/util/IngredientSupplierHelper;getIngredientSupplier(Ljava/lang/Object;Lmezz/jei/api/recipe/category/IRecipeCategory;Lmezz/jei/api/runtime/IIngredientManager;)Lmezz/jei/library/ingredients/IIngredientSupplier;"), require = 0)
    private <T> IIngredientSupplier gtcsolo$indexArcaneAnvil(T recipe, IRecipeCategory<T> category,
                                                            IIngredientManager ingredientManager) {
        if (category.getClass().getName().equals("io.redspace.ironsspellbooks.jei.ArcaneAnvilRecipeCategory")
                && Boolean.parseBoolean(System.getProperty("gtcsolo.optimization.jei.arcaneAnvilIndex", "true"))) {
            IIngredientSupplier supplier = ArcaneAnvilIngredientIndex.tryCreate(recipe, ingredientManager);
            if (supplier != null) return supplier;
        }
        return IngredientSupplierHelper.getIngredientSupplier(recipe, category, ingredientManager);
    }
}
