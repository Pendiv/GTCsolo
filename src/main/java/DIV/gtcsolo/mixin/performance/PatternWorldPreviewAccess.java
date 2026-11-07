package DIV.gtcsolo.mixin.performance;

import com.gregtechceu.gtceu.api.pattern.BlockPattern;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.function.BiFunction;
import java.util.function.Consumer;

/** Reuses native preview facing selection against sparse neighbour information. */
@Mixin(value = BlockPattern.class, remap = false)
public interface PatternWorldPreviewAccess {
    @Invoker("resetFacing")
    void gtcsolo$worldPreviewResetFacing(BlockPos position, BlockState state, Direction facing,
                                       BiFunction<BlockPos, Direction, Boolean> checker, Consumer<BlockState> consumer);
}
