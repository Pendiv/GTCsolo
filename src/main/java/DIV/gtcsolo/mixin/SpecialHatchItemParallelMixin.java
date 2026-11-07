package DIV.gtcsolo.mixin;

import DIV.gtcsolo.machine.overpower.HatchParallelSupport;
import com.gregtechceu.gtceu.api.capability.recipe.IRecipeCapabilityHolder;
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ItemRecipeCapability.class, remap = false)
public abstract class SpecialHatchItemParallelMixin {
    @Inject(method = "limitParallel", at = @At("HEAD"), cancellable = true, remap = false)
    private void gtcsolo$specialOutputCapacity(GTRecipe recipe, IRecipeCapabilityHolder holder, int multiplier,
                                             CallbackInfoReturnable<Integer> callback) {
        Integer limit = HatchParallelSupport.itemLimit(recipe, holder, multiplier);
        if (limit != null) callback.setReturnValue(limit);
    }
}
