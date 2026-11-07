package DIV.gtcsolo.mixin;

import DIV.gtcsolo.machine.overpower.HatchParallelSupport;
import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.IRecipeCapabilityHolder;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = FluidRecipeCapability.class, remap = false)
public abstract class SpecialHatchFluidParallelMixin {
    @Inject(method = "limitParallel", at = @At("HEAD"), cancellable = true, remap = false)
    private void gtcsolo$creativeOutputCapacity(GTRecipe recipe, IRecipeCapabilityHolder holder, int multiplier,
                                               CallbackInfoReturnable<Integer> callback) {
        if (HatchParallelSupport.hasCreativeFluidOutput(holder)) callback.setReturnValue(multiplier);
    }
}
