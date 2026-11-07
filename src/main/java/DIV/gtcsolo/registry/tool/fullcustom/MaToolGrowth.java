package DIV.gtcsolo.registry.tool.fullcustom;

import com.gregtechceu.gtceu.api.item.tool.ToolHelper;
import com.gregtechceu.gtceu.api.item.tool.aoe.AoESymmetrical;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Counts completed server-side mining, including the native area-mining path. */
public final class MaToolGrowth {
    public static final String BLOCKS_TAG = "gtcsolo.ma_blocks_mined";
    public static final AoESymmetrical INITIAL_AREA = AoESymmetrical.of(2, 2, 0);
    private static final int MAX_LEVEL = (int) StrictMath.pow(Long.MAX_VALUE / 3.0, 1.0 / 2.4);
    private static final AoESymmetrical[] AREAS = {
        INITIAL_AREA,
        AoESymmetrical.of(2, 2, 1),
        AoESymmetrical.of(2, 2, 2),
        AoESymmetrical.of(2, 2, 3),
        AoESymmetrical.of(2, 2, 4),
        AoESymmetrical.of(3, 3, 4),
        AoESymmetrical.of(3, 3, 5),
        AoESymmetrical.of(3, 3, 6),
        AoESymmetrical.of(4, 4, 6),
        AoESymmetrical.of(4, 4, 7),
        AoESymmetrical.of(4, 4, 8)
    };

    private MaToolGrowth() {}

    public static long blocksMined(CompoundTag tag) {
        return tag == null ? 0 : Math.max(0, tag.getLong(BLOCKS_TAG));
    }

    public static long requiredBlocks(int level) {
        if (level <= 0) return 0;
        return (long) Math.ceil(3.0 * StrictMath.pow(level, 2.4) - 1.0);
    }

    public static int level(long blocks) {
        long count = Math.max(0, blocks);
        int level = Math.min(MAX_LEVEL, (int) StrictMath.pow((count + 1.0) / 3.0, 1.0 / 2.4));
        while (level > 0 && requiredBlocks(level) > count) level--;
        while (level < MAX_LEVEL && requiredBlocks(level + 1) <= count) level++;
        return level;
    }

    public static int level(ItemStack stack) {
        return level(blocksMined(stack.getTag()));
    }

    public static long remainingBlocks(ItemStack stack) {
        long count = blocksMined(stack.getTag());
        int level = level(count);
        return level == MAX_LEVEL ? 0 : requiredBlocks(level + 1) - count;
    }

    public static AoESymmetrical maximumArea(int level) {
        return AREAS[Math.min(AREAS.length - 1, Math.max(0, level) / 3)];
    }

    public static void updateArea(CompoundTag tag) {
        var maximum = maximumArea(level(blocksMined(tag)));
        var behavior = tag.getCompound(ToolHelper.BEHAVIOURS_TAG_KEY);
        updateAxis(
                behavior,
                ToolHelper.MAX_AOE_COLUMN_KEY,
                ToolHelper.AOE_COLUMN_KEY,
                maximum.column,
                INITIAL_AREA.column);
        updateAxis(
                behavior,
                ToolHelper.MAX_AOE_ROW_KEY,
                ToolHelper.AOE_ROW_KEY,
                maximum.row,
                INITIAL_AREA.row);
        updateAxis(
                behavior,
                ToolHelper.MAX_AOE_LAYER_KEY,
                ToolHelper.AOE_LAYER_KEY,
                maximum.layer,
                INITIAL_AREA.layer);
        tag.put(ToolHelper.BEHAVIOURS_TAG_KEY, behavior);
    }

    private static void updateAxis(
            CompoundTag tag, String maximumKey, String currentKey, int maximum, int initial) {
        tag.putInt(maximumKey, maximum);
        int current = tag.contains(currentKey, Tag.TAG_INT) ? tag.getInt(currentKey) : initial;
        tag.putInt(currentKey, Math.max(0, Math.min(current, maximum)));
    }

    public static void recordBreak(ItemStack stack, Player player, BlockState previousState) {
        if (!(stack.getItem() instanceof MaCrystalToolItem)
                || player.level().isClientSide
                || player.isCreative()
                || player.isSpectator()
                || previousState.isAir()
                || previousState.getBlock() instanceof LiquidBlock) return;
        var tag = stack.getOrCreateTag();
        long count = blocksMined(tag);
        if (count < Long.MAX_VALUE) {
            tag.putLong(BLOCKS_TAG, count + 1);
            if (level(count) != level(count + 1)) updateArea(tag);
        }
    }
}
