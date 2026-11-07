package DIV.gtcsolo.mixin.performance;

import DIV.gtcsolo.integration.jei.performance.LazyNativeMultiblockPreviews;
import com.lowdragmc.lowdraglib.jei.ModularUIRecipeCategory;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.recipe.IFocusGroup;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ModularUIRecipeCategory.class, remap = false)
public abstract class MultiblockPreviewIngredientIndexMixin {
    @Inject(method = "setRecipe", at = @At("HEAD"), cancellable = true)
    private void gtcsolo$indexMetadataOnly(IRecipeLayoutBuilder builder, Object recipe, IFocusGroup focuses, CallbackInfo ci) {
        if (LazyNativeMultiblockPreviews.indexOnly((ModularUIRecipeCategory<?>) (Object) this, builder, recipe, focuses)) ci.cancel();
    }
}
