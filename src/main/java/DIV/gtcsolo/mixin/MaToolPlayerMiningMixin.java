package DIV.gtcsolo.mixin;

import DIV.gtcsolo.registry.tool.fullcustom.MaToolGrowth;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.level.block.state.BlockState;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(ServerPlayerGameMode.class)
public abstract class MaToolPlayerMiningMixin {
    @Shadow @Final protected ServerPlayer player;

    // Forge's removal method returns the actual result after all cancellation checks.
    @Inject(
            method = "removeBlock",
            remap = false,
            at = @At("RETURN"),
            locals = LocalCapture.CAPTURE_FAILHARD)
    private void gtcsolo$countSingleBreak(
            BlockPos pos,
            boolean canHarvest,
            CallbackInfoReturnable<Boolean> callback,
            BlockState state,
            boolean removed) {
        if (removed) MaToolGrowth.recordBreak(player.getMainHandItem(), player, state);
    }
}
