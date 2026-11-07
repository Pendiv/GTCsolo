package DIV.gtcsolo.torcherino.mixin;

import DIV.gtcsolo.torcherino.client.SliderBindings;
import net.minecraft.client.gui.components.AbstractSliderButton;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import torcherino.client.screen.widgets.GradatedSliderWidget;

@Mixin(value = GradatedSliderWidget.class, remap = false)
public abstract class GradatedSliderMixin {
    @Inject(method = {"setValue", "m_93611_"}, at = @At("TAIL"))
    private void tiered$limitKeyboardValue(double value, CallbackInfo ci) {
        SliderBindings.reconcile((AbstractSliderButton) (Object) this);
    }
}
