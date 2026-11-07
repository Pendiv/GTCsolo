package DIV.gtcsolo.mixin;

import DIV.gtcsolo.integration.mekanism.capability.UniversalOutputRouting;
import com.gregtechceu.gtceu.api.capability.recipe.IRecipeCapabilityHolder;
import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.modifier.ParallelLogic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Predicate;

/** Applies shared physical capacity after GT has limited all other output capabilities. */
@Mixin(value = ParallelLogic.class, remap = false)
public abstract class UniversalParallelMixin {
    @Inject(method = "limitByOutputMerging", at = @At("RETURN"), cancellable = true, remap = false)
    private static void gtcsolo$limitUniversalOutputs(IRecipeCapabilityHolder holder, GTRecipe recipe,
                                                     int parallelLimit,
                                                     Predicate<RecipeCapability<?>> canVoid,
                                                     CallbackInfoReturnable<Integer> callback) {
        int originalLimit = callback.getReturnValue();
        if (originalLimit <= 0) return;
        // Shared fluid/gas storage must not overbook even when generic output voiding is enabled.
        callback.setReturnValue(UniversalOutputRouting.limitParallel(holder, recipe, originalLimit));
    }
}
