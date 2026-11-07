package DIV.gtcsolo.util;

import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

/** Save full stack/capability data while preserving counts larger than the vanilla byte. */
public final class LargeStackNbt {
    private LargeStackNbt() {}

    public static CompoundTag save(List<ItemStack> stacks) {
        ListTag entries = new ListTag();
        for (int i = 0; i < stacks.size(); i++) {
            ItemStack stack = stacks.get(i);
            if (stack.isEmpty()) continue;
            CompoundTag entry = stack.save(new CompoundTag());
            entry.putInt("Slot", i);
            entry.putInt("Count", stack.getCount());
            entries.add(entry);
        }
        CompoundTag tag = new CompoundTag();
        tag.put("Items", entries);
        tag.putInt("Size", stacks.size());
        return tag;
    }

    public static void loadInto(CompoundTag tag, List<ItemStack> stacks) {
        for (int i = 0; i < stacks.size(); i++) stacks.set(i, ItemStack.EMPTY);
        ListTag entries = tag.getList("Items", Tag.TAG_COMPOUND);
        for (int i = 0; i < entries.size(); i++) {
            CompoundTag entry = entries.getCompound(i);
            int slot = entry.getInt("Slot");
            int count = entry.getInt("Count");
            if (slot < 0 || slot >= stacks.size() || count <= 0) continue;
            CompoundTag normalized = entry.copy();
            normalized.putByte("Count", (byte) 1);
            ItemStack stack = ItemStack.of(normalized);
            if (!stack.isEmpty()) {
                stack.setCount(count);
                stacks.set(slot, stack);
            }
        }
    }
}
