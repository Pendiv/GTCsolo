package DIV.gtcsolo.mixin.performance;

import DIV.gtcsolo.client.performance.ClientFrameDiagnostics;
import DIV.gtcsolo.client.performance.ClientFrameDiagnostics.Phase;
import DIV.gtcsolo.performance.PerformanceDiagnostics;
import com.mojang.blaze3d.systems.RenderSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderSystem.class)
public abstract class RenderSystemFrameDiagnosticsMixin {
    @Inject(method = "limitDisplayFPS", at = @At("HEAD"), remap = false)
    private static void gtcsolo$beginFrameLimit(int limit, CallbackInfo ci) {
        if (PerformanceDiagnostics.isEnabled()) ClientFrameDiagnostics.beginPhase(Phase.LIMIT);
    }

    @Inject(method = "limitDisplayFPS", at = @At("RETURN"), remap = false)
    private static void gtcsolo$endFrameLimit(int limit, CallbackInfo ci) {
        if (PerformanceDiagnostics.isEnabled()) ClientFrameDiagnostics.endPhase(Phase.LIMIT);
    }

    @Inject(method = "flipFrame", at = @At(value = "INVOKE",
            target = "Lorg/lwjgl/glfw/GLFW;glfwSwapBuffers(J)V"), remap = false)
    private static void gtcsolo$beginSwapBuffers(long window, CallbackInfo ci) {
        if (PerformanceDiagnostics.isEnabled()) ClientFrameDiagnostics.beginPhase(Phase.SWAP_BUFFERS);
    }

    @Inject(method = "flipFrame", at = @At(value = "INVOKE",
            target = "Lorg/lwjgl/glfw/GLFW;glfwSwapBuffers(J)V", shift = At.Shift.AFTER), remap = false)
    private static void gtcsolo$endSwapBuffers(long window, CallbackInfo ci) {
        if (PerformanceDiagnostics.isEnabled()) ClientFrameDiagnostics.endPhase(Phase.SWAP_BUFFERS);
    }
}
