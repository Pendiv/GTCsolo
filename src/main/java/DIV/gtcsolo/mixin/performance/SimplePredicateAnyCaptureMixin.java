package DIV.gtcsolo.mixin.performance;

import DIV.gtcsolo.performance.UnconstrainedPatternCells;
import com.gregtechceu.gtceu.api.pattern.predicates.SimplePredicate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = SimplePredicate.class, remap = false)
public abstract class SimplePredicateAnyCaptureMixin {
    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void gtcsolo$captureOriginalAny(CallbackInfo callback) {
        UnconstrainedPatternCells.captureCanonicalAny(SimplePredicate.ANY);
    }
}
