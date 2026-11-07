package DIV.gtcsolo.mixin.performance;

import DIV.gtcsolo.integration.jei.performance.JeiOptimizationOptions;
import DIV.gtcsolo.integration.jei.performance.NativeGtRecipeIngredientIndex;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.integration.jei.recipe.GTRecipeJEICategory;
import com.lowdragmc.lowdraglib.jei.ModularUIRecipeCategory;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.recipe.IFocusGroup;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Only ingredient-only scans of native GT recipe categories can use the guarded path. */
@Mixin(value = ModularUIRecipeCategory.class, remap = false)
public abstract class GTRecipeIngredientIndexMixin {
    @Inject(method = "setRecipe", at = @At("HEAD"), cancellable = true)
    private void gtcsolo$indexWithoutRecipeScreen(IRecipeLayoutBuilder builder, Object recipe,
                                                 IFocusGroup focuses, CallbackInfo ci) {
        if (!JeiOptimizationOptions.recipeIndexEnabled() || NativeGtRecipeIngredientIndex.isVerifyingNative()) return;
        if (((Object) this).getClass() == GTRecipeJEICategory.class && recipe instanceof GTRecipe gtRecipe
                && NativeGtRecipeIngredientIndex.tryIndex((ModularUIRecipeCategory<?>) (Object) this,
                        builder, gtRecipe, focuses)) ci.cancel();
    }
}
