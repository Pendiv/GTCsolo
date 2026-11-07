package DIV.gtcsolo.machine;

import net.minecraft.world.item.Item;
import net.minecraftforge.items.IItemHandler;

import java.util.IdentityHashMap;
import java.util.Map;

/** Output samples are grouped by item ID, independently of slots, stack sizes and NBT. */
public final class IterativeSampleInventory {
    private IterativeSampleInventory() {}

    public static Map<Item, Long> count(IItemHandler inventory) {
        Map<Item, Long> counts = new IdentityHashMap<>();
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            var stack = inventory.getStackInSlot(slot);
            if (!stack.isEmpty()) counts.merge(stack.getItem(), (long) stack.getCount(), Long::sum);
        }
        return counts;
    }

    public static boolean qualifies(Map<Item, Long> counts, Item output) {
        return !counts.isEmpty() && counts.getOrDefault(output, 0L) >= counts.size();
    }
}
