package DIV.gtcsolo.item;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.NotNull;

/**
 * 設置すると指定 Fluid の流体源 (water source / lava source) になるアイテム。
 * バケツと違い、設置後は空バケツに戻らずアイテムを 1 個消費するだけ (snowball 系の挙動)。
 */
public class FluidPlacingItem extends Item {

    private final Fluid fluid;

    public FluidPlacingItem(Fluid fluid, Properties properties) {
        super(properties);
        this.fluid = fluid;
    }

    @Override
    @NotNull
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        BlockPos clicked = ctx.getClickedPos();
        BlockPos placePos = clicked.relative(ctx.getClickedFace());

        BlockState existing = level.getBlockState(placePos);
        // 空気 or 置換可能 (背の高い草など) のみ設置許可。流体が既にある場合は不可。
        if (!existing.canBeReplaced(fluid) || !existing.getFluidState().isEmpty()) {
            return InteractionResult.FAIL;
        }

        if (!level.isClientSide) {
            BlockState fluidBlock = fluid.defaultFluidState().createLegacyBlock();
            // setBlock with flag 11 (BLOCK_UPDATE | NOTIFY_CLIENTS | RERENDER_MAIN_THREAD)
            level.setBlock(placePos, fluidBlock, 11);
            // 効果音
            level.playSound(null, placePos,
                    fluid == Fluids.LAVA ? SoundEvents.BUCKET_EMPTY_LAVA : SoundEvents.BUCKET_EMPTY,
                    SoundSource.BLOCKS, 1.0f, 1.0f);
        }

        Player player = ctx.getPlayer();
        if (player == null || !player.getAbilities().instabuild) {
            ItemStack held = ctx.getItemInHand();
            held.shrink(1);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
