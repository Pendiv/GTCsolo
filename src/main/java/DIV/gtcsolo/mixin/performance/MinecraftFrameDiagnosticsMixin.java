package DIV.gtcsolo.mixin.performance;

import DIV.gtcsolo.client.performance.ClientFrameDiagnostics;
import DIV.gtcsolo.client.performance.ClientFrameDiagnostics.Phase;
import DIV.gtcsolo.performance.PerformanceDiagnostics;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Observes existing calls only; no native call, argument, return value or exception is replaced. */
@Mixin(Minecraft.class)
public abstract class MinecraftFrameDiagnosticsMixin {
    @Inject(method = "runTick", at = @At("HEAD"))
    private void gtcsolo$beginFrame(boolean renderLevel, CallbackInfo ci) {
        if (PerformanceDiagnostics.isEnabled()) ClientFrameDiagnostics.beginFrame();
    }

    @Inject(method = "runTick", at = @At("RETURN"))
    private void gtcsolo$endFrame(boolean renderLevel, CallbackInfo ci) {
        if (PerformanceDiagnostics.isEnabled()) ClientFrameDiagnostics.endFrame();
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void gtcsolo$beginClientTick(CallbackInfo ci) {
        if (PerformanceDiagnostics.isEnabled()) ClientFrameDiagnostics.beginPhase(Phase.TICK);
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private void gtcsolo$endClientTick(CallbackInfo ci) {
        if (PerformanceDiagnostics.isEnabled()) ClientFrameDiagnostics.endPhase(Phase.TICK);
    }

    @Inject(method = "runTick", at = @At(value = "INVOKE",
            target = "Lcom/mojang/blaze3d/systems/RenderSystem;clear(IZ)V", remap = false))
    private void gtcsolo$beginRender(boolean renderLevel, CallbackInfo ci) {
        if (PerformanceDiagnostics.isEnabled()) ClientFrameDiagnostics.beginPhase(Phase.RENDER);
    }

    @Inject(method = "runTick", at = @At(value = "INVOKE",
            target = "Lcom/mojang/blaze3d/platform/Window;updateDisplay()V"))
    private void gtcsolo$endRender(boolean renderLevel, CallbackInfo ci) {
        if (PerformanceDiagnostics.isEnabled()) ClientFrameDiagnostics.endPhase(Phase.RENDER);
    }
}
