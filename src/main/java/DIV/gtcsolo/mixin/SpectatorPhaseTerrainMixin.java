package DIV.gtcsolo.mixin;

import DIV.gtcsolo.effect.SpectatorPhase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(LevelRenderer.class)
public abstract class SpectatorPhaseTerrainMixin {

    @ModifyArg(method = "renderLevel", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/LevelRenderer;setupRender(Lnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/culling/Frustum;ZZ)V"), index = 3)
    private boolean gtcsolo$renderTerrainFromInsideBlocks(boolean spectator) {
        var player = Minecraft.getInstance().player;
        return spectator || player != null && SpectatorPhase.isActive(player);
    }
}
