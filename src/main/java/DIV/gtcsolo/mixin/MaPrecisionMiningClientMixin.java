package DIV.gtcsolo.mixin;

import DIV.gtcsolo.registry.tool.fullcustom.MaToolMode;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Precision mode must not inherit the normal five-tick pause between instant block breaks. */
@Mixin(MultiPlayerGameMode.class)
public abstract class MaPrecisionMiningClientMixin {
    @Shadow private int destroyDelay;

    @Inject(
            method = {"startDestroyBlock", "continueDestroyBlock"},
            at = @At("HEAD"))
    private void gtcsolo$precisionDelay(
            BlockPos pos, Direction face, CallbackInfoReturnable<Boolean> callback) {
        var player = Minecraft.getInstance().player;
        if (player != null
                && MaToolMode.isPrecision(player.getMainHandItem())
                && MaToolMode.isSuitableTarget(
                        player.getMainHandItem(), player.level().getBlockState(pos))) {
            destroyDelay = 0;
        }
    }
}
