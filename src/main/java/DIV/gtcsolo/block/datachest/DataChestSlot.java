package DIV.gtcsolo.block.datachest;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;

/**
 * DataChest 専用 Slot。 maxStackSize を {@link DataChestItemHandler#MULTIPLIER} 倍で
 * report し、 large count 格納を許容する。
 */
public class DataChestSlot extends SlotItemHandler {

    public DataChestSlot(IItemHandler handler, int index, int x, int y) {
        super(handler, index, x, y);
    }

    @Override
    public int getMaxStackSize() {
        ItemStack current = getItem();
        int base = current.isEmpty() ? 64 : current.getMaxStackSize();
        return base * DataChestItemHandler.MULTIPLIER;
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        return stack.getMaxStackSize() * DataChestItemHandler.MULTIPLIER;
    }

    /**
     * vanilla の {@code Slot.setChanged()} → {@code SlotItemHandler.setChanged()} は
     * {@link DataChestItemHandler#onContentsChanged} を呼ばない (= chain が切れてる)。
     * vanilla の click 経路で {@code ItemStack.grow/shrink + setChanged} される時に
     * BE sync が走らないと client 側が古いままになるので、 ここで明示的に hook を発火。
     */
    @Override
    public void setChanged() {
        super.setChanged();
        if (getItemHandler() instanceof DataChestItemHandler dch) {
            dch.markChanged(getSlotIndex());
        }
    }
}
