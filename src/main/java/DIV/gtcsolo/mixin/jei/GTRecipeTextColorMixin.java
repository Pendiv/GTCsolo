package DIV.gtcsolo.mixin.jei;

import DIV.gtcsolo.integration.jei.JeiRecipeBackgrounds;
import DIV.gtcsolo.integration.jei.StyledJeiRecipeWidget;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.common.recipe.condition.CleanroomCondition;
import com.gregtechceu.gtceu.integration.xei.widgets.GTRecipeWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Keep the styling boundary at JEI, rather than changing GT's shared XEI widget globally. */
@Pseudo
@Mixin(targets = "com.gregtechceu.gtceu.integration.jei.recipe.GTRecipeWrapper", remap = false)
public abstract class GTRecipeTextColorMixin {
    @Redirect(
            method = "<init>(Lcom/gregtechceu/gtceu/api/recipe/GTRecipe;)V",
            at = @At(value = "NEW", target = "com/gregtechceu/gtceu/integration/xei/widgets/GTRecipeWidget"),
            require = 1,
            remap = false)
    private static GTRecipeWidget gtcsolo$createRecipeWidget(GTRecipe recipe) {
        if (JeiRecipeBackgrounds.informationColorForType(recipe.recipeType) == null
                && JeiRecipeBackgrounds.backgroundForType(recipe.recipeType) == null
                && recipe.conditions.stream().noneMatch(CleanroomCondition.class::isInstance)) {
            return new GTRecipeWidget(recipe);
        }
        return new StyledJeiRecipeWidget(recipe);
    }
}
