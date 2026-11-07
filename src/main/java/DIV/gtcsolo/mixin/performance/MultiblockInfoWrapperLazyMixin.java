package DIV.gtcsolo.mixin.performance;

import DIV.gtcsolo.integration.jei.performance.LazyNativeMultiblockPreviews;
import DIV.gtcsolo.integration.jei.performance.LazyNativePreviewRecipe;
import com.gregtechceu.gtceu.integration.jei.multipage.MultiblockInfoWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = MultiblockInfoWrapper.class, remap = false)
public abstract class MultiblockInfoWrapperLazyMixin implements LazyNativePreviewRecipe {
    @Unique private LazyNativeMultiblockPreviews.State gtcsolo$lazyNativePreview;

    @Override public LazyNativeMultiblockPreviews.State gtcsolo$getLazyNativePreview() { return gtcsolo$lazyNativePreview; }
    @Override public void gtcsolo$setLazyNativePreview(LazyNativeMultiblockPreviews.State state) { gtcsolo$lazyNativePreview = state; }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void gtcsolo$prepareSearchMetadata(CallbackInfo ci) {
        LazyNativeMultiblockPreviews.finishConstruction((MultiblockInfoWrapper) (Object) this);
    }
}
