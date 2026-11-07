package DIV.gtcsolo.block.datachest;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;

/**
 * DataChest 用の ItemStackHandler。 各 slot に
 * {@code stack.getMaxStackSize() × MULTIPLIER} 個まで格納可能。
 *
 * <p>vanilla ItemStack の count は int なので最大 ~21M (= 64 × 335542) は問題なく格納可能。
 * ただし vanilla UI 操作で 1 click 移動できるのは maxStackSize 単位、 大量移動は
 * shift+クリ等で {@link DataChestMenu#quickMoveStack} 経由。
 */
public class DataChestItemHandler extends ItemStackHandler {

    public static final int MULTIPLIER = 335542;
    public static final int SLOT_COUNT = 81;

    private final DataChestBlockEntity owner;

    public DataChestItemHandler(DataChestBlockEntity owner) {
        super(SLOT_COUNT);
        this.owner = owner;
    }

    @Override
    public int getSlotLimit(int slot) {
        // 既に slot にアイテムが入ってる場合は その maxStackSize × MULTIPLIER
        ItemStack existing = this.stacks.get(slot);
        if (!existing.isEmpty()) {
            return expandedLimit(existing.getMaxStackSize());
        }
        // 空 slot の場合は safe default (= 64 × MULTIPLIER)、 insert 時に再計算
        return expandedLimit(64);
    }

    @Override
    public int getStackLimit(int slot, ItemStack stack) {
        return expandedLimit(stack.getMaxStackSize());
    }

    public static int expandedLimit(int normalLimit) {
        return (int) Math.min(Integer.MAX_VALUE, Math.max(0L, (long) normalLimit) * MULTIPLIER);
    }

    @Override
    protected void onContentsChanged(int slot) {
        super.onContentsChanged(slot);
        if (owner != null) {
            owner.setChanged();
            owner.syncToClient();
        }
    }

    /**
     * 外部から「この slot 変更されたよ」 を通知して onContentsChanged を発火するヘルパ。
     * {@link DataChestSlot#setChanged} から呼ばれる
     * (= vanilla の SlotItemHandler.setChanged → IItemHandler 経路が無いため自前 hook)。
     */
    public void markChanged(int slot) {
        onContentsChanged(slot);
    }

    // ─── NBT 自前 serialize / deserialize ───
    // vanilla の ItemStack.save は Count を putByte で書く (= 127 越えで wrap)。
    // ItemStackHandler.serializeNBT は ItemStack.save を呼ぶため、 ここで完全 override し
    // Count を putInt で書き出す (= 32bit 保持で 21M 対応)。
    // 読込時は Count を getInt で退避し、通常の ItemStack / ForgeCaps 読込後に復元する。

    @Override
    public CompoundTag serializeNBT() {
        return DIV.gtcsolo.util.LargeStackNbt.save(stacks);
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        setSize(SLOT_COUNT);
        DIV.gtcsolo.util.LargeStackNbt.loadInto(nbt, stacks);
        onLoad();
    }
}
