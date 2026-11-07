package DIV.gtcsolo.mixin;

import DIV.gtcsolo.render.material.portal.GalacticaWorldRenderer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
public abstract class GalacticaClientLevelMixin {
    @Inject(method = "sendBlockUpdated", at = @At("TAIL"))
    private void gtcsolo$updateGalactica(BlockPos pos, BlockState oldState, BlockState newState, int flags, CallbackInfo ci) {
        GalacticaWorldRenderer.blockChanged((ClientLevel) (Object) this, pos, newState);
    }
}
