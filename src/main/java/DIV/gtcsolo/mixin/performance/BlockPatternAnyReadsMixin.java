package DIV.gtcsolo.mixin.performance;

import DIV.gtcsolo.performance.UnconstrainedPatternCells;
import com.gregtechceu.gtceu.api.pattern.BlockPattern;
import com.gregtechceu.gtceu.api.pattern.MultiblockState;
import com.gregtechceu.gtceu.api.pattern.TraceabilityPredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = BlockPattern.class, remap = false)
public abstract class BlockPatternAnyReadsMixin {
    @Redirect(method = "checkPatternAt(Lcom/gregtechceu/gtceu/api/pattern/MultiblockState;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;Lnet/minecraft/core/Direction;ZZ)Z",
            at = @At(value = "INVOKE", target = "Lcom/gregtechceu/gtceu/api/pattern/MultiblockState;update(Lnet/minecraft/core/BlockPos;Lcom/gregtechceu/gtceu/api/pattern/TraceabilityPredicate;)Z"))
    private boolean gtcsolo$updateWithoutUnconstrainedWorldRead(MultiblockState state, BlockPos position, TraceabilityPredicate predicate) {
        var access = (UnconstrainedPatternCells.StateAccess) state;
        if (UnconstrainedPatternCells.canSkip(state, predicate)) {
            access.gtcsolo$updateUnconstrained(position, predicate);
            return true;
        }
        return access.gtcsolo$nativeUpdate(position, predicate);
    }

    @Redirect(method = "checkPatternAt(Lcom/gregtechceu/gtceu/api/pattern/MultiblockState;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;Lnet/minecraft/core/Direction;ZZ)Z",
            at = @At(value = "INVOKE", target = "Lcom/gregtechceu/gtceu/api/pattern/MultiblockState;getTileEntity()Lnet/minecraft/world/level/block/entity/BlockEntity;"))
    private BlockEntity gtcsolo$ignoreUnrelatedBlockEntity(MultiblockState state) {
        if (((UnconstrainedPatternCells.StateAccess) state).gtcsolo$isUnconstrained()) return null;
        return state.getTileEntity();
    }

    @Redirect(method = "checkPatternAt(Lcom/gregtechceu/gtceu/api/pattern/MultiblockState;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;Lnet/minecraft/core/Direction;ZZ)Z",
            at = @At(value = "INVOKE", target = "Lcom/gregtechceu/gtceu/api/pattern/MultiblockState;getBlockState()Lnet/minecraft/world/level/block/state/BlockState;"))
    private BlockState gtcsolo$ignoreUnrelatedActiveBlock(MultiblockState state) {
        if (((UnconstrainedPatternCells.StateAccess) state).gtcsolo$isUnconstrained()) return Blocks.AIR.defaultBlockState();
        return state.getBlockState();
    }
}
