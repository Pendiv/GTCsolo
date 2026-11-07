package DIV.gtcsolo.machine.mega;

import DIV.gtcsolo.performance.FixedPatternChunkRetry;
import DIV.gtcsolo.performance.UnconstrainedPatternCells;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.pattern.MultiblockState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/** Native production/formation behavior with a cheap retry for a known unloaded required chunk. */
public final class MegaMixerMachine extends WorkableElectricMultiblockMachine {
    private FixedPatternChunkRetry chunkRetry;

    public MegaMixerMachine(IMachineBlockEntity holder, Object... args) { super(holder, args); }

    @Override
    public void asyncCheckPattern(long periodID) {
        var state = getMultiblockState();
        boolean due = (state.hasError() || !isFormed()) && (getHolder().getOffset() + periodID) % 4 == 0;
        if (!due || !(getLevel() instanceof ServerLevel level)) {
            super.asyncCheckPattern(periodID);
            return;
        }
        var dimension = level.dimension().location();
        var center = getPos();
        var front = getFrontFacing();
        var up = getUpwardsFacing();
        boolean flipped = isFlipped();
        boolean fixedOrientation = hasFrontFacing() && !allowFlip();
        boolean unconstrainedFailure = UnconstrainedPatternCells.isPureAny(state.predicate);
        if (unconstrainedFailure && chunkRetry != null) chunkRetry.clear();
        // GT deliberately retains isFormed during chunk unload; UNLOAD_ERROR already blocks recipe logic.
        // Manual/synchronous checks and every healthy formed machine still follow native matching.
        if (fixedOrientation && !unconstrainedFailure && chunkRetry != null && state.error == MultiblockState.UNLOAD_ERROR
                && chunkRetry.shouldSkip(dimension, center, front, up, flipped, level::isLoaded)) return;

        super.asyncCheckPattern(periodID);
        // A native ANY unload must not become a proof that survives enabling unconstrained-cell reads.
        if (fixedOrientation && state.error == MultiblockState.UNLOAD_ERROR
                && !UnconstrainedPatternCells.isPureAny(state.predicate)) {
            if (chunkRetry == null) chunkRetry = MegaMixerPattern.newChunkRetry();
            BlockPos required;
            try { required = state.getPos(); }
            catch (NullPointerException noNativePosition) { required = null; }
            chunkRetry.rememberUnloaded(dimension, center, front, up, flipped, required, level::isLoaded);
        } else if (chunkRetry != null) {
            chunkRetry.clear();
        }
    }
}
