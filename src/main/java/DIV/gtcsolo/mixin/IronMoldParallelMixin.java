package DIV.gtcsolo.mixin;

import DIV.gtcsolo.recipe.mold.MoldRecipeSupport;
import com.gregtechceu.gtceu.api.capability.recipe.IRecipeCapabilityHolder;
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ItemRecipeCapability.class, remap = false)
public abstract class IronMoldParallelMixin {
    @Inject(method = "getMaxParallelRatio", at = @At("HEAD"), cancellable = true)
    private void gtcsolo$limitMoldWear(IRecipeCapabilityHolder holder, GTRecipe recipe, int limit,
                                      CallbackInfoReturnable<Integer> callback) {
        if (MoldRecipeSupport.hasMolds(recipe)) callback.setReturnValue(MoldRecipeSupport.maxParallel(holder, recipe, limit));
    }
}
