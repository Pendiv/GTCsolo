package DIV.gtcsolo.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * 不明なブロック (= 特性「媒介野」が生成する罠ブロック)。 仮登録枠 {@code gtcsolo:unknown} を転用。
 *
 * <p>各ブロックは BlockEntity ({@link MediatorFieldBlockEntity}) に「元のブロック」と「デバフ 1 種」を持つ。
 * プレイヤーが踏むとデバフを付与し CT 10 秒に入る (同種デバフ保持中は効果時間を加算)。
 * <b>破壊されると消えるのではなく元のブロックに戻る</b>。 適正ツールは鍬 (mineable/hoe tag)。
 */
public class MediatorFieldBlock extends BaseEntityBlock {

    public MediatorFieldBlock(Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;   // BaseEntityBlock 既定の INVISIBLE を解除
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MediatorFieldBlockEntity(pos, state);
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (!level.isClientSide && entity instanceof ServerPlayer player
                && level.getBlockEntity(pos) instanceof MediatorFieldBlockEntity be) {
            be.trigger(player);
        }
        super.stepOn(level, pos, state, entity);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (!state.is(newState.getBlock())) {
            BlockState original = level.getBlockEntity(pos) instanceof MediatorFieldBlockEntity be
                    ? be.getOriginal() : null;
            super.onRemove(state, level, pos, newState, moving);
            // 破壊 (= 空気への置換) なら元のブロックへ復元する
            if (!level.isClientSide && original != null && !original.isAir() && newState.isAir()) {
                level.setBlock(pos, original, 3);
            }
            return;
        }
        super.onRemove(state, level, pos, newState, moving);
    }
}
