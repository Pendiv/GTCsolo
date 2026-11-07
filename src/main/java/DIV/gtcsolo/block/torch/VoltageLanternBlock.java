package DIV.gtcsolo.block.torch;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Vanilla standing/hanging and waterlogging behaviour with the common voltage ward. */
public final class VoltageLanternBlock extends LanternBlock implements EntityBlock, VoltageWardLight {
    private final VoltageTorchTier tier;

    public VoltageLanternBlock(Properties properties, VoltageTorchTier tier) {
        super(properties);
        this.tier = tier;
    }

    @Override
    public VoltageTorchTier tier() { return tier; }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new VoltageTorchBlockEntity(pos, state);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moved) {
        super.onPlace(state, level, pos, oldState, moved);
        if (level instanceof ServerLevel server) TorchWardData.get(server).put(pos, tier.level());
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && level instanceof ServerLevel server) {
            TorchWardData.get(server).remove(pos);
        }
        super.onRemove(state, level, pos, newState, moved);
    }
}
