package DIV.gtcsolo.mixin.performance;

import DIV.gtcsolo.performance.UnconstrainedPatternCells;
import com.gregtechceu.gtceu.api.pattern.MultiblockState;
import com.gregtechceu.gtceu.api.pattern.TraceabilityPredicate;
import com.gregtechceu.gtceu.api.pattern.error.PatternError;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = MultiblockState.class, remap = false)
public abstract class MultiblockStateAnyAccessMixin implements UnconstrainedPatternCells.StateAccess {
    @Shadow private BlockPos pos;
    @Shadow private BlockState blockState;
    @Shadow private BlockEntity tileEntity;
    @Shadow private boolean tileEntityInitialized;
    @Shadow public TraceabilityPredicate predicate;
    @Shadow public PatternError error;
    @Shadow protected abstract boolean update(BlockPos position, TraceabilityPredicate predicate);
    @Unique private boolean gtcsolo$unconstrained;

    @Override
    public boolean gtcsolo$nativeUpdate(BlockPos position, TraceabilityPredicate predicate) {
        gtcsolo$unconstrained = false;
        return update(position, predicate);
    }

    @Override
    public void gtcsolo$updateUnconstrained(BlockPos position, TraceabilityPredicate predicate) {
        this.pos = position;
        this.blockState = null;
        this.tileEntity = null;
        this.tileEntityInitialized = false;
        this.predicate = predicate;
        this.error = null;
        this.gtcsolo$unconstrained = true;
    }

    @Override public void gtcsolo$clearUnconstrained() { gtcsolo$unconstrained = false; }
    @Override public boolean gtcsolo$isUnconstrained() { return gtcsolo$unconstrained; }
}
