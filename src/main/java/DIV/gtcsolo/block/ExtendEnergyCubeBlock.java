package DIV.gtcsolo.block;

import DIV.gtcsolo.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

public class ExtendEnergyCubeBlock extends BaseEntityBlock {

    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final BooleanProperty BOOSTED = BooleanProperty.create("boosted");

    public ExtendEnergyCubeBlock() {
        super(BlockBehaviour.Properties.of()
                .strength(5.0f, 12.0f)
                .sound(SoundType.METAL)
                .requiresCorrectToolForDrops()
                .lightLevel(state -> state.getValue(BOOSTED) ? 15 : 0));
        registerDefaultState(stateDefinition.any()
                .setValue(POWERED, false)
                .setValue(BOOSTED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(POWERED, BOOSTED);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                  Player player, InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;

        if (player.isShiftKeyDown()) {
            // shift右クリック: BOOSTED トグル
            boolean nowBoosted = !state.getValue(BOOSTED);
            level.setBlock(pos, state.setValue(BOOSTED, nowBoosted), 3);
            return InteractionResult.CONSUME;
        }

        // 通常右クリック: UI を開く
        if (level.getBlockEntity(pos) instanceof ExtendEnergyCubeBlockEntity be
                && player instanceof ServerPlayer serverPlayer) {
            NetworkHooks.openScreen(serverPlayer, be, be::writeScreenOpenData);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos,
                                net.minecraft.world.level.block.Block block, BlockPos fromPos, boolean isMoving) {
        if (level.isClientSide) return;
        boolean powered = level.hasNeighborSignal(pos);
        if (state.getValue(POWERED) != powered) {
            level.setBlock(pos, state.setValue(POWERED, powered), 3);
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ExtendEnergyCubeBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return createTickerHelper(type, ModBlockEntities.EXTEND_ENERGY_CUBE.get(),
                ExtendEnergyCubeBlockEntity::tick);
    }
}