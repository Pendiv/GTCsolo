package DIV.gtcsolo.mixin;

import DIV.gtcsolo.registry.tool.fullcustom.MaToolGrowth;

import com.gregtechceu.gtceu.api.item.tool.ToolHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ToolHelper.class, remap = false)
public abstract class MaToolGrowthMixin {
    @Inject(method = "removeBlockRoutine", at = @At("RETURN"))
    private static void gtcsolo$countAreaBreak(
            BlockState state,
            Level level,
            ServerPlayer player,
            BlockPos pos,
            boolean playSound,
            CallbackInfoReturnable<Boolean> callback) {
        if (callback.getReturnValueZ()) {
            MaToolGrowth.recordBreak(player.getMainHandItem(), player, state);
        }
    }
}
