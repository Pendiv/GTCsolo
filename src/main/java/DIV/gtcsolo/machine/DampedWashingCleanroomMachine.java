package DIV.gtcsolo.machine;

import DIV.gtcsolo.mixin.CleanroomDimensionsAccess;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.pattern.error.PatternStringError;
import com.gregtechceu.gtceu.common.machine.multiblock.electric.CleanroomMachine;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

public final class DampedWashingCleanroomMachine extends CleanroomMachine {
    private static final int MAX_WIDTH = 16;
    private static final int MAX_RADIUS = MAX_WIDTH / 2;
    private static final int MAX_DEPTH = 31;
    private boolean dimensionsValid;

    public DampedWashingCleanroomMachine(IMachineBlockEntity holder) {
        super(holder);
    }

    @Override
    public void updateStructureDimensions() {
        Level level = getLevel();
        dimensionsValid = false;
        if (level == null) return;

        int west = findBoundary(level, Direction.WEST);
        int east = findBoundary(level, Direction.EAST);
        int north = findBoundary(level, Direction.NORTH);
        int south = findBoundary(level, Direction.SOUTH);
        int depth = findBoundary(level, Direction.DOWN);
        dimensionsValid = validSpan(west, east) && validSpan(north, south) && depth >= MIN_DEPTH;

        var dimensions = (CleanroomDimensionsAccess) (CleanroomMachine) this;
        // The native cleanroom's rows and columns advance north and west from their starting edges.
        dimensions.gtcsolo$setLeftDistance(dimensionsValid ? east : MIN_RADIUS);
        dimensions.gtcsolo$setRightDistance(dimensionsValid ? west : MIN_RADIUS);
        dimensions.gtcsolo$setFrontDistance(dimensionsValid ? south : MIN_RADIUS);
        dimensions.gtcsolo$setBackDistance(dimensionsValid ? north : MIN_RADIUS);
        dimensions.gtcsolo$setFloorDepth(dimensionsValid ? depth : MIN_DEPTH);
    }

    private int findBoundary(Level level, Direction direction) {
        boolean floor = direction == Direction.DOWN;
        int limit = floor ? MAX_DEPTH : MAX_RADIUS;
        BlockPos.MutableBlockPos pos = getPos().mutable();
        for (int distance = 1; distance <= limit; distance++) {
            if (level instanceof ServerLevel && !level.hasChunkAt(pos.relative(direction))) return 0;
            if (floor ? isBlockFloor(level, pos, direction) : isBlockEdge(level, pos, direction)) return distance;
        }
        return 0;
    }

    private static boolean validSpan(int first, int second) {
        return first >= MIN_RADIUS && second >= MIN_RADIUS
                && first + second + 1 <= MAX_WIDTH && Math.abs(first - second) <= 1;
    }

    @Override
    public boolean checkPattern() {
        var pattern = getPattern();
        var state = getMultiblockState();
        if (state.getController() != this) return false;
        if (!dimensionsValid) {
            state.setError(new PatternStringError("gtcsolo.multiblock.error.damped_washing_dimensions"));
            return false;
        }
        // The ceiling scan and the pattern use the same world axes, including on even-sized rooms.
        return pattern.checkPatternAt(state, getPos(), Direction.NORTH, Direction.NORTH, false, false);
    }
}
