package DIV.gtcsolo.mixin.performance;

import DIV.gtcsolo.performance.PatternCheckProfiler;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Times one existing asynchronous try-lock request, including native/grouped matching. */
@Mixin(value = MultiblockControllerMachine.class, remap = false)
public abstract class MultiblockPatternProfilerMixin {
    @Redirect(method = "asyncCheckPattern", at = @At(value = "INVOKE", target =
            "Lcom/gregtechceu/gtceu/api/machine/multiblock/MultiblockControllerMachine;checkPatternWithTryLock()Z"))
    private boolean gtcsolo$profilePattern(MultiblockControllerMachine controller) {
        var state = controller.getMultiblockState();
        long started = PatternCheckProfiler.begin(state);
        boolean matched = false;
        boolean threw = true;
        try {
            matched = controller.checkPatternWithTryLock();
            threw = false;
            return matched;
        } finally {
            PatternCheckProfiler.finish(controller, state, started, matched, threw);
        }
    }
}
