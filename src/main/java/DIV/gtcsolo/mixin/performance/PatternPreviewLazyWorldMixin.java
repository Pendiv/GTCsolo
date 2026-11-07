package DIV.gtcsolo.mixin.performance;

import DIV.gtcsolo.integration.jei.performance.DeferredPatternPreviewWidget;
import DIV.gtcsolo.integration.jei.performance.LazyNativeMultiblockPreviews;
import com.gregtechceu.gtceu.api.gui.widget.PatternPreviewWidget;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.pattern.MultiblockShapeInfo;
import com.gregtechceu.gtceu.config.ConfigHolder;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

/** Empty native widgets are registration placeholders; full widgets still execute native code. */
@Mixin(value = PatternPreviewWidget.class, remap = false)
public abstract class PatternPreviewLazyWorldMixin implements DeferredPatternPreviewWidget {
    @Shadow @Final public MultiblockMachineDefinition controllerDefinition;
    @Unique private boolean gtcsolo$deferredPatternPreview;

    @Override public boolean gtcsolo$isDeferredPatternPreview() { return gtcsolo$deferredPatternPreview; }

    @Redirect(method = "<init>", at = @At(value = "INVOKE",
            target = "Ljava/util/Map;computeIfAbsent(Ljava/lang/Object;Ljava/util/function/Function;)Ljava/lang/Object;"))
    @SuppressWarnings({"rawtypes", "unchecked"})
    private Object gtcsolo$deferWorld(Map cache, Object key, Function loader) {
        if (LazyNativeMultiblockPreviews.shouldDefer(controllerDefinition)) {
            gtcsolo$deferredPatternPreview = true;
            return LazyNativeMultiblockPreviews.emptyNativePatterns();
        }
        return cache.computeIfAbsent(key, loader);
    }

    @Redirect(method = "<init>", at = @At(value = "FIELD",
            target = "Lcom/gregtechceu/gtceu/config/ConfigHolder$ClientConfigs;useVBO:Z"))
    private boolean gtcsolo$skipPlaceholderVbo(ConfigHolder.ClientConfigs client) {
        return !LazyNativeMultiblockPreviews.shouldDefer(controllerDefinition) && client.useVBO;
    }

    @Inject(method = "loadControllerFormed", at = @At("HEAD"), cancellable = true)
    private void gtcsolo$metadataDoesNotForm(CallbackInfo ci) {
        if (LazyNativeMultiblockPreviews.isMetadataPass()) ci.cancel();
    }

    @Redirect(method = "lambda$new$2", at = @At(value = "INVOKE",
            target = "Lcom/gregtechceu/gtceu/api/machine/MultiblockMachineDefinition;getMatchingShapes()Ljava/util/List;"))
    private List<MultiblockShapeInfo> gtcsolo$useFrozenShapeMetadata(MultiblockMachineDefinition definition) {
        return LazyNativeMultiblockPreviews.shapesForNative(definition);
    }

    @Inject(method = "getPatternWidget", at = @At("HEAD"), cancellable = true)
    private static void gtcsolo$useReservedRegion(MultiblockMachineDefinition definition,
                                                  CallbackInfoReturnable<PatternPreviewWidget> cir) {
        PatternPreviewWidget widget = LazyNativeMultiblockPreviews.materializeReserved(definition);
        if (widget != null) cir.setReturnValue(widget);
    }
}
