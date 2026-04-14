package DIV.gtcsolo.block.wen;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** WEN Input/Output Port ブロック。FE Capability付きBlockEntity。 */
public class WENPortBlock extends BaseEntityBlock {

    public WENPortBlock() {
        super(BlockBehaviour.Properties.of()
                .strength(5.0f, 12.0f)
                .sound(SoundType.METAL)
                .requiresCorrectToolForDrops());
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WENPortBlockEntity(pos, state);
    }
}