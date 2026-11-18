package DIV.gtcsolo.mixin.jei;

import DIV.gtcsolo.machine.ChemicalReactionTemperatures;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.api.recipe.ui.GTRecipeTypeUI;
import com.gregtechceu.gtceu.integration.xei.widgets.GTRecipeWidget;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = GTRecipeTypeUI.class, remap = false)
public abstract class ChemicalReactionTemperatureUiMixin {
    @Shadow @Final private GTRecipeType recipeType;

    @Inject(method = "getPropertyHeightShift", at = @At("RETURN"), cancellable = true)
    private void gtcsolo$reserveTemperatureLine(CallbackInfoReturnable<Integer> callback) {
        if (ChemicalReactionTemperatures.supports(recipeType)) {
            callback.setReturnValue(callback.getReturnValueI() + GTRecipeWidget.LINE_HEIGHT);
        }
    }
}
