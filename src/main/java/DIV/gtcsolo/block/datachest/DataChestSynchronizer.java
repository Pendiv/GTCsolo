package DIV.gtcsolo.block.datachest;

import net.minecraft.core.NonNullList;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerSynchronizer;
import net.minecraft.world.item.ItemStack;

/** Ordinary container packets carry display stacks; the BE's int-count NBT carries actual storage. */
public final class DataChestSynchronizer implements ContainerSynchronizer {
    private final ContainerSynchronizer delegate;

    public DataChestSynchronizer(ContainerSynchronizer delegate) { this.delegate = delegate; }

    public static ItemStack displayStack(ItemStack stack) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        int limit = Math.max(1, Math.min(127, stack.getMaxStackSize()));
        return stack.copyWithCount(Math.min(stack.getCount(), limit));
    }

    @Override
    public void sendInitialData(AbstractContainerMenu menu, NonNullList<ItemStack> stacks, ItemStack carried, int[] data) {
        NonNullList<ItemStack> display = NonNullList.withSize(stacks.size(), ItemStack.EMPTY);
        for (int i = 0; i < stacks.size(); i++) {
            display.set(i, i < DataChestMenu.CHEST_SLOTS ? displayStack(stacks.get(i)) : stacks.get(i));
        }
        delegate.sendInitialData(menu, display, carried, data);
    }

    @Override
    public void sendSlotChange(AbstractContainerMenu menu, int slot, ItemStack stack) {
        delegate.sendSlotChange(menu, slot,
                slot >= 0 && slot < DataChestMenu.CHEST_SLOTS ? displayStack(stack) : stack);
    }

    @Override
    public void sendCarriedChange(AbstractContainerMenu menu, ItemStack carried) { delegate.sendCarriedChange(menu, carried); }

    @Override
    public void sendDataChange(AbstractContainerMenu menu, int index, int value) { delegate.sendDataChange(menu, index, value); }
}
