package DIV.gtcsolo.mixin.performance;

import DIV.gtcsolo.performance.PatternCoordinateTransform;
import com.gregtechceu.gtceu.api.pattern.BlockPattern;
import com.gregtechceu.gtceu.api.pattern.util.RelativeDirection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = BlockPattern.class, remap = false)
public abstract class BlockPatternCoordinatesMixin {
    @Shadow @Final public RelativeDirection[] structureDir;

    @Shadow
    private BlockPos setActualRelativeOffset(int x, int y, int z, Direction front, Direction up, boolean flipped) {
        throw new AssertionError("Mixin shadow");
    }

    @Redirect(method = "checkPatternAt(Lcom/gregtechceu/gtceu/api/pattern/MultiblockState;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;Lnet/minecraft/core/Direction;ZZ)Z",
            at = @At(value = "INVOKE", target = "Lcom/gregtechceu/gtceu/api/pattern/BlockPattern;setActualRelativeOffset(IIILnet/minecraft/core/Direction;Lnet/minecraft/core/Direction;Z)Lnet/minecraft/core/BlockPos;"))
    private BlockPos gtcsolo$scalarOffset(BlockPattern owner, int x, int y, int z,
                                          Direction front, Direction up, boolean flipped) {
        if (!PatternCoordinateTransform.isEnabled() || !PatternCoordinateTransform.supports(front, up)) {
            return setActualRelativeOffset(x, y, z, front, up, flipped);
        }
        return PatternCoordinateTransform.offset(structureDir, x, y, z, front, up, flipped);
    }
}
