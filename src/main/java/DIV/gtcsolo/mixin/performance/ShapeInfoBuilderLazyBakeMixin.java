package DIV.gtcsolo.mixin.performance;

import DIV.gtcsolo.integration.jei.performance.LazyNativeMultiblockPreviews;
import com.gregtechceu.gtceu.api.pattern.MultiblockShapeInfo;
import com.gregtechceu.gtceu.api.pattern.MultiblockShapeInfo.ShapeInfoBuilder;
import com.lowdragmc.lowdraglib.utils.BlockInfo;
import com.lowdragmc.lowdraglib.utils.Builder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ShapeInfoBuilder.class, remap = false)
public abstract class ShapeInfoBuilderLazyBakeMixin extends Builder<BlockInfo, ShapeInfoBuilder> {
    @Inject(method = "build", at = @At("HEAD"), cancellable = true)
    private void gtcsolo$captureNativeShape(CallbackInfoReturnable<MultiblockShapeInfo> cir) {
        if (((Object) this).getClass() == ShapeInfoBuilder.class) {
            MultiblockShapeInfo deferred = LazyNativeMultiblockPreviews.deferShapeBake(shape, symbolMap);
            if (deferred != null) cir.setReturnValue(deferred);
        }
    }
}
