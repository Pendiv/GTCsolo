package DIV.gtcsolo.registry.tool.fullcustom;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;

public enum MaToolMode {
    BULK("bulk"),
    PRECISION("precision"),
    OFF("off");

    public static final String TAG = "gtcsolo.ma_tool_mode";
    public static final float PRECISION_TICKS = 0.2F;
    private final String id;

    MaToolMode(String id) {
        this.id = id;
    }

    public Component label() {
        return Component.translatable("gtcsolo.ma_tool.mode." + id);
    }

    public MaToolMode cycle(int steps) {
        return values()[Math.floorMod(ordinal() + steps % values().length, values().length)];
    }

    public static MaToolMode read(CompoundTag tag) {
        if (tag != null) {
            String id = tag.getString(TAG);
            for (MaToolMode mode : values()) if (mode.id.equals(id)) return mode;
        }
        return BULK;
    }

    public static MaToolMode get(ItemStack stack) {
        return read(stack.getTag());
    }

    public static boolean set(ItemStack stack, MaToolMode mode) {
        if (!(stack.getItem() instanceof MaCrystalToolItem)) return false;
        stack.getOrCreateTag().putString(TAG, mode.id);
        return true;
    }

    public static void normalize(CompoundTag tag) {
        tag.putString(TAG, read(tag).id);
    }

    public static boolean isPrecision(ItemStack stack) {
        return stack.getItem() instanceof MaCrystalToolItem && get(stack) == PRECISION;
    }

    public static boolean isSuitableTarget(ItemStack stack, BlockState state) {
        return stack.getItem() instanceof MaCrystalToolItem
                && !state.isAir()
                && !(state.getBlock() instanceof LiquidBlock)
                && stack.isCorrectToolForDrops(state);
    }
}
