package DIV.gtcsolo.block.torch;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.TorchBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class VoltageTorchBlock extends TorchBlock implements EntityBlock, VoltageWardLight {
    private final VoltageTorchTier tier;

    public VoltageTorchBlock(Properties properties, VoltageTorchTier tier) {
        super(properties, ParticleTypes.FLAME);
        this.tier = tier;
    }

    public VoltageTorchTier tier() {
        return tier;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new VoltageTorchBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moved) {
        super.onPlace(state, level, pos, oldState, moved);
        if (level instanceof ServerLevel server) {
            TorchWardData.get(server).put(pos, tier.level());
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && level instanceof ServerLevel server) {
            TorchWardData.get(server).remove(pos);
        }
        super.onRemove(state, level, pos, newState, moved);
    }
}
