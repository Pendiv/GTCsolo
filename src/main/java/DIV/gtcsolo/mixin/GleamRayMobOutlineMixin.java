package DIV.gtcsolo.mixin;

import DIV.gtcsolo.client.gleam.GleamRayMobOutlines;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.Entity;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class GleamRayMobOutlineMixin {
    @Inject(method = "renderLevel", at = @At("HEAD"))
    private void gtcsolo$beginMobOutlines(CallbackInfo ci) {
        GleamRayMobOutlines.beginFrame();
    }

    @ModifyVariable(method = "renderEntity", at = @At("HEAD"), argsOnly = true)
    private MultiBufferSource gtcsolo$captureMobOutline(MultiBufferSource original, Entity entity,
            double x, double y, double z, float partialTick, PoseStack pose, MultiBufferSource source) {
        return GleamRayMobOutlines.wrap(entity, partialTick, original);
    }

    @Inject(method = "renderLevel", at = @At("RETURN"))
    private void gtcsolo$drawMobOutlines(PoseStack pose, float partialTick, long finishTime, boolean renderBlockOutline,
            Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f projection, CallbackInfo ci) {
        GleamRayMobOutlines.finishFrame(partialTick);
    }

    @Inject(method = "close", at = @At("HEAD"))
    private void gtcsolo$releaseMobOutlines(CallbackInfo ci) {
        GleamRayMobOutlines.release();
    }
}
