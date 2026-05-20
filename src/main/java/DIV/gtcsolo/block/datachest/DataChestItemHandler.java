package DIV.gtcsolo.block.datachest;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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
            return existing.getMaxStackSize() * MULTIPLIER;
        }
        // 空 slot の場合は safe default (= 64 × MULTIPLIER)、 insert 時に再計算
        return 64 * MULTIPLIER;
    }

    @Override
    public int getStackLimit(int slot, ItemStack stack) {
        return Math.min(getSlotLimit(slot), stack.getMaxStackSize() * MULTIPLIER);
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
    // deserializeNBT 側も ItemStack.of を経由せず自前で getInt で読む。

    @Override
    public CompoundTag serializeNBT() {
        ListTag list = new ListTag();
        for (int i = 0; i < stacks.size(); i++) {
            ItemStack s = stacks.get(i);
            if (s.isEmpty()) continue;
            CompoundTag tag = new CompoundTag();
            tag.putInt("Slot", i);
            ResourceLocation rl = BuiltInRegistries.ITEM.getKey(s.getItem());
            tag.putString("id", rl == null ? "minecraft:air" : rl.toString());
            tag.putInt("Count", s.getCount());        // ★ putInt (= 32bit、 byte cap 回避)
            if (s.hasTag()) tag.put("tag", s.getTag().copy());
            list.add(tag);
        }
        CompoundTag nbt = new CompoundTag();
        nbt.put("Items", list);
        nbt.putInt("Size", stacks.size());
        return nbt;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        setSize(nbt.contains("Size", Tag.TAG_INT) ? nbt.getInt("Size") : stacks.size());
        // 全 slot を一旦 EMPTY にリセット (= 旧 entry の残留防止)
        for (int i = 0; i < stacks.size(); i++) stacks.set(i, ItemStack.EMPTY);
        ListTag list = nbt.getList("Items", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag tag = list.getCompound(i);
            int slot = tag.getInt("Slot");
            if (slot < 0 || slot >= stacks.size()) continue;
            ResourceLocation rl;
            try { rl = new ResourceLocation(tag.getString("id")); }
            catch (Exception e) { continue; }
            Item item = BuiltInRegistries.ITEM.get(rl);
            if (item == Items.AIR) continue;
            int count = tag.getInt("Count");          // ★ getInt (= 32bit)
            if (count <= 0) continue;
            ItemStack s = new ItemStack(item, count);
            if (tag.contains("tag", Tag.TAG_COMPOUND)) s.setTag(tag.getCompound("tag"));
            stacks.set(slot, s);
        }
        onLoad();
    }
}
