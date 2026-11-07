package DIV.gtcsolo.mixin;

import DIV.gtcsolo.render.material.InfinityCableTextures;
import com.gregtechceu.gtceu.common.block.CableBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Infinity's colored sheath must not be multiplied by the stock 0x404040 tint. */
@Mixin(value = CableBlock.class, remap = false)
public abstract class InfinityCableColorMixin {
    @Inject(method = "tinted", at = @At("HEAD"), cancellable = true, remap = false)
    private void gtcsolo$infinityCableColor(BlockState state, BlockAndTintGetter level, BlockPos pos,
                                           int index, CallbackInfoReturnable<Integer> cir) {
        CableBlock block = (CableBlock) (Object) this;
        if (index == 0 && block.pipeType.isCable && InfinityCableTextures.matches(block.material)) {
            cir.setReturnValue(0xFFFFFF);
        }
    }
}
