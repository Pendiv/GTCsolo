package DIV.gtcsolo.mixin.performance;

import DIV.gtcsolo.performance.PatternPredicateLoop;
import com.gregtechceu.gtceu.api.pattern.BlockPattern;
import com.gregtechceu.gtceu.api.pattern.MultiblockState;
import com.gregtechceu.gtceu.api.pattern.TraceabilityPredicate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = BlockPattern.class, remap = false)
public abstract class BlockPatternPredicateLoopMixin {
    @Redirect(method = "checkPatternAt(Lcom/gregtechceu/gtceu/api/pattern/MultiblockState;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;Lnet/minecraft/core/Direction;ZZ)Z",
            at = @At(value = "INVOKE", target = "Lcom/gregtechceu/gtceu/api/pattern/TraceabilityPredicate;test(Lcom/gregtechceu/gtceu/api/pattern/MultiblockState;)Z"))
    private boolean gtcsolo$testWithoutStream(TraceabilityPredicate predicate, MultiblockState state) {
        if (!PatternPredicateLoop.isEnabled()) return predicate.test(state);
        return PatternPredicateLoop.test(predicate, state);
    }
}
