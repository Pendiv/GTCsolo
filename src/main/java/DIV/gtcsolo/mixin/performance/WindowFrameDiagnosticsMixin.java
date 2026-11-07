package DIV.gtcsolo.mixin.performance;

import DIV.gtcsolo.client.performance.ClientFrameDiagnostics;
import DIV.gtcsolo.client.performance.ClientFrameDiagnostics.Phase;
import DIV.gtcsolo.performance.PerformanceDiagnostics;
import com.mojang.blaze3d.platform.Window;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Window.class)
public abstract class WindowFrameDiagnosticsMixin {
    @Inject(method = "updateDisplay", at = @At("HEAD"))
    private void gtcsolo$beginDisplayUpdate(CallbackInfo ci) {
        if (PerformanceDiagnostics.isEnabled()) ClientFrameDiagnostics.beginPhase(Phase.DISPLAY_UPDATE);
    }

    @Inject(method = "updateDisplay", at = @At("RETURN"))
    private void gtcsolo$endDisplayUpdate(CallbackInfo ci) {
        if (PerformanceDiagnostics.isEnabled()) ClientFrameDiagnostics.endPhase(Phase.DISPLAY_UPDATE);
    }
}
