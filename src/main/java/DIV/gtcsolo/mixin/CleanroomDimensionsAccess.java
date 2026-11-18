package DIV.gtcsolo.mixin;

import com.gregtechceu.gtceu.common.machine.multiblock.electric.CleanroomMachine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = CleanroomMachine.class, remap = false)
public interface CleanroomDimensionsAccess {
    @Accessor("lDist") void gtcsolo$setLeftDistance(int distance);
    @Accessor("rDist") void gtcsolo$setRightDistance(int distance);
    @Accessor("fDist") void gtcsolo$setFrontDistance(int distance);
    @Accessor("bDist") void gtcsolo$setBackDistance(int distance);
    @Accessor("hDist") void gtcsolo$setFloorDepth(int distance);
}
