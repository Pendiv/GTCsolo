package DIV.gtcsolo.mixin.performance;

import DIV.gtcsolo.integration.jei.performance.LazyNativeMultiblockPreviews;
import DIV.gtcsolo.integration.jei.performance.ModularWrapperWidgetAccess;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.jei.ModularWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ModularWrapper.class, remap = false)
public abstract class ModularWrapperLazyPreviewMixin implements ModularWrapperWidgetAccess {
    @Shadow protected Widget widget;

    @Override public Widget gtcsolo$getRawWrappedWidget() { return widget; }
    @Override public void gtcsolo$setRawWrappedWidget(Widget widget) { this.widget = widget; }

    @Inject(method = "getWidget", at = @At("HEAD"))
    private void gtcsolo$materializeBeforeTypedGetter(CallbackInfoReturnable<Widget> cir) {
        LazyNativeMultiblockPreviews.ensureLoaded((ModularWrapper<?>) (Object) this);
    }

    @Inject(method = {"setRecipeWidget", "setEmiRecipeWidget", "draw", "updateScreen"}, at = @At("HEAD"))
    private void gtcsolo$materializeBeforeUiUse(CallbackInfo ci) {
        LazyNativeMultiblockPreviews.ensureLoaded((ModularWrapper<?>) (Object) this);
    }
}
