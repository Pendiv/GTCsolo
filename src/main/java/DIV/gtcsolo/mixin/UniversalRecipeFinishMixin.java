package DIV.gtcsolo.mixin;

import DIV.gtcsolo.integration.mekanism.capability.UniversalOutputRouting;
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Retains completed work until its chemical outputs can be delivered together. */
@Mixin(value = RecipeLogic.class, remap = false)
public abstract class UniversalRecipeFinishMixin {
    @Redirect(method = "serverTick", at = @At(value = "INVOKE",
            target = "Lcom/gregtechceu/gtceu/api/machine/trait/RecipeLogic;onRecipeFinish()V"),
            remap = false)
    private void gtcsolo$waitForUniversalOutputSpace(RecipeLogic logic) {
        GTRecipe recipe = logic.getLastRecipe();
        if (recipe != null && !UniversalOutputRouting.canFinish(recipe, logic.machine, logic.getChanceCaches())) {
            // Keep progress == duration. serverTick retries finish without running another work tick.
            logic.setWaiting(UniversalOutputRouting.blockedReason(logic.machine));
            return;
        }
        // Dynamic dispatch preserves specialized RecipeLogic implementations.
        logic.onRecipeFinish();
    }
}
