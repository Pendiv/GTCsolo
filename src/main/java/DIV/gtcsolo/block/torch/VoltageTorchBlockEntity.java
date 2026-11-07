package DIV.gtcsolo.block.torch;

import DIV.gtcsolo.registry.VoltageTorches;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Non-ticking anchor: also restores the ward index when a saved chunk is loaded. */
public final class VoltageTorchBlockEntity extends BlockEntity {
    public VoltageTorchBlockEntity(BlockPos pos, BlockState state) {
        super(VoltageTorches.BLOCK_ENTITY.get(), pos, state);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level instanceof ServerLevel server) {
            int tier = VoltageTorches.tier(getBlockState());
            if (tier > 0) {
                TorchWardData.get(server).put(worldPosition, tier);
            }
        }
    }
}
