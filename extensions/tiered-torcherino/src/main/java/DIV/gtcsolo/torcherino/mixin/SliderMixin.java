package DIV.gtcsolo.torcherino.mixin;

import DIV.gtcsolo.torcherino.client.SliderAccess;
import DIV.gtcsolo.torcherino.client.SliderBindings;
import net.minecraft.client.gui.components.AbstractSliderButton;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractSliderButton.class)
public abstract class SliderMixin implements SliderAccess {
    @Accessor("value") public abstract void tiered$setValue(double value);
    @Invoker("updateMessage") public abstract void tiered$updateMessage();
    @Inject(method = "setValue", at = @At("TAIL"))
    private void tiered$limitMouseValue(double value, CallbackInfo ci) {
        SliderBindings.reconcile((AbstractSliderButton) (Object) this);
    }
}
