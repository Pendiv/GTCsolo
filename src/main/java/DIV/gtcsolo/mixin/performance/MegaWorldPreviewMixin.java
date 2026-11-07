package DIV.gtcsolo.mixin.performance;

import DIV.gtcsolo.client.performance.MegaWorldPreview;
import DIV.gtcsolo.client.performance.NativeWorldPreviewBatches;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;
import com.gregtechceu.gtceu.client.renderer.MultiblockInWorldPreviewRenderer;
import com.lowdragmc.lowdraglib.utils.TrackedDummyWorld;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@Mixin(value = MultiblockInWorldPreviewRenderer.class, remap = false)
public abstract class MegaWorldPreviewMixin {
    @Shadow private static TrackedDummyWorld LEVEL;
    @Shadow private static Thread THREAD;
    @Shadow private static Set<BlockPos> BLOCK_ENTITIES;
    @Shadow @Final private static AtomicInteger LEFT_TICK;

    @Inject(method = "showPreview", at = @At("HEAD"), cancellable = true)
    private static void gtcsolo$stageKnownMega(BlockPos position, MultiblockControllerMachine controller, int duration, CallbackInfo ci) {
        if (MegaWorldPreview.tryShow(position, controller, duration)) ci.cancel();
        else if (controller.getDefinition().isRenderWorldPreview()) MegaWorldPreview.clear();
    }
    @Inject(method = "cleanPreview", at = @At("HEAD"))
    private static void gtcsolo$closeOwnedPreview(CallbackInfo ci) { MegaWorldPreview.onNativeClear(); }
    @Inject(method = "removePreview", at = @At("HEAD"))
    private static void gtcsolo$removeOwnedPreview(BlockPos position, CallbackInfo ci) { MegaWorldPreview.remove(position); }
    @Inject(method = "onClientTick", at = @At("TAIL"))
    private static void gtcsolo$checkWorld(CallbackInfo ci) { MegaWorldPreview.tick(); }
    @Inject(method = "renderInWorldPreview", at = @At("HEAD"), cancellable = true)
    private static void gtcsolo$prepareFrame(PoseStack pose, Camera camera, float partialTicks, CallbackInfo ci) {
        if (!MegaWorldPreview.beforeRender()) ci.cancel();
    }
    @Redirect(method = "renderInWorldPreview", at = @At(value = "INVOKE",
            target = "Lcom/gregtechceu/gtceu/client/renderer/MultiblockInWorldPreviewRenderer;getBUFFERS()[Lcom/mojang/blaze3d/vertex/VertexBuffer;"))
    private static VertexBuffer[] gtcsolo$ownedBuffers() {
        VertexBuffer[] buffers = MegaWorldPreview.buffers();
        return buffers == null ? MultiblockInWorldPreviewRenderer.getBUFFERS() : buffers;
    }
    @Redirect(method = "renderInWorldPreview", at = @At(value = "INVOKE",
            target = "Lcom/mojang/blaze3d/vertex/VertexBuffer;draw()V", remap = true))
    private static void gtcsolo$drawOwnedBatches(VertexBuffer receiver) {
        if (!MegaWorldPreview.draw(receiver)) receiver.draw();
    }

    // Only replace the native worker's four-field completion tail. A worker that passed its
    // final interrupt check must not republish after cleanPreview or a different preview.
    @Redirect(method = "lambda$prepareBuffers$2", at = @At(value = "FIELD",
            target = "Lcom/gregtechceu/gtceu/client/renderer/MultiblockInWorldPreviewRenderer;BLOCK_ENTITIES:Ljava/util/Set;",
            opcode = org.objectweb.asm.Opcodes.PUTSTATIC), require = 1, allow = 1)
    private static void gtcsolo$deferNativePublication(Set<BlockPos> blockEntities, TrackedDummyWorld expectedLevel,
                                                       Collection<BlockPos> renderedBlocks, int duration) {
        Thread worker = Thread.currentThread();
        Minecraft.getInstance().execute(() -> {
            if (MegaWorldPreview.isActive() || LEVEL != expectedLevel || THREAD != worker || worker.isInterrupted()) return;
            BLOCK_ENTITIES = blockEntities;
            THREAD = null;
            LEFT_TICK.set(duration);
            NativeWorldPreviewBatches.publishCompiledState();
        });
    }

    @Redirect(method = "lambda$prepareBuffers$2", at = @At(value = "INVOKE",
            target = "Ljava/util/concurrent/atomic/AtomicReference;set(Ljava/lang/Object;)V"), require = 1, allow = 1)
    private static void gtcsolo$deferNativeState(AtomicReference<?> state, Object value) {}

    @Redirect(method = "lambda$prepareBuffers$2", at = @At(value = "FIELD",
            target = "Lcom/gregtechceu/gtceu/client/renderer/MultiblockInWorldPreviewRenderer;THREAD:Ljava/lang/Thread;",
            opcode = org.objectweb.asm.Opcodes.PUTSTATIC), require = 1, allow = 1)
    private static void gtcsolo$deferNativeThread(Thread thread) {}

    @Redirect(method = "lambda$prepareBuffers$2", at = @At(value = "INVOKE",
            target = "Ljava/util/concurrent/atomic/AtomicInteger;set(I)V"), require = 1, allow = 1)
    private static void gtcsolo$deferNativeDuration(AtomicInteger ticks, int duration) {}
}
