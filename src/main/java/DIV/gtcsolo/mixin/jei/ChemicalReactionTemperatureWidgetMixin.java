package DIV.gtcsolo.mixin.jei;

import DIV.gtcsolo.machine.ChemicalReactionTemperatures;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.integration.xei.widgets.GTRecipeWidget;
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = GTRecipeWidget.class, remap = false)
public abstract class ChemicalReactionTemperatureWidgetMixin {
    @Shadow @Final private int xOffset;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void gtcsolo$showRecommendedTemperature(GTRecipe recipe, CallbackInfo callback) {
        int kelvin = ChemicalReactionTemperatures.recommendedKelvin(recipe);
        if (kelvin > 0) {
            var widget = (GTRecipeWidget) (Object) this;
            widget.addWidget(new LabelWidget(3 - xOffset, widget.getSize().height - GTRecipeWidget.LINE_HEIGHT,
                    Component.translatable("gtcsolo.recipe.recommended_temperature", kelvin)).setDropShadow(true));
        }
    }
}
