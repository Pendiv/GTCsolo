package DIV.gtcsolo.mixin;

import DIV.gtcsolo.machine.ChemicalReactionTemperatures;
import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine;
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = RecipeLogic.class, remap = false)
public abstract class ChemicalReactionTemperatureMixin {
    @Shadow @Final public IRecipeLogicMachine machine;

    @Redirect(method = "setupRecipe", at = @At(value = "FIELD", opcode = Opcodes.GETFIELD,
            target = "Lcom/gregtechceu/gtceu/api/recipe/GTRecipe;duration:I"))
    private int gtcsolo$applyRecommendedTemperature(GTRecipe recipe) {
        // Only the active batch's duration changes; cached recipes can be reused at another temperature.
        return ChemicalReactionTemperatures.processingTicks(recipe, machine.self());
    }
}
