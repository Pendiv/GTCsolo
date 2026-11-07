package DIV.gtcsolo.mixin;

import DIV.gtcsolo.integration.mekanism.capability.UniversalOutputRouting;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.capability.recipe.IRecipeCapabilityHolder;
import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.content.Content;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Map;

/** Keeps fluid and gas allocation inside one complete GT contents operation. */
@Mixin(value = GTRecipe.class, remap = false)
public abstract class UniversalRecipeOutputMixin {
    @Inject(method = "matchRecipeContents", at = @At("HEAD"), cancellable = true, remap = false)
    private void gtcsolo$matchUniversalOutputs(IO io, IRecipeCapabilityHolder holder,
                                              Map<RecipeCapability<?>, List<Content>> contents,
                                              boolean isTick,
                                              CallbackInfoReturnable<GTRecipe.ActionResult> callback) {
        GTRecipe.ActionResult result = UniversalOutputRouting.interceptMatch(
                (GTRecipe) (Object) this, io, holder, contents, isTick);
        // The helper's detached proxy holder bypasses this hook on native reentry.
        if (result != null) callback.setReturnValue(result);
    }

    @Inject(method = "handleRecipe", at = @At("HEAD"), cancellable = true, remap = false)
    private void gtcsolo$handleUniversalOutputs(IO io, IRecipeCapabilityHolder holder,
                                               boolean isTick,
                                               Map<RecipeCapability<?>, List<Content>> contents,
                                               Map<RecipeCapability<?>, Object2IntMap<?>> chanceCaches,
                                               CallbackInfoReturnable<Boolean> callback) {
        Boolean result = UniversalOutputRouting.interceptHandle(
                (GTRecipe) (Object) this, io, holder, isTick, contents, chanceCaches);
        if (result != null) callback.setReturnValue(result);
    }
}
