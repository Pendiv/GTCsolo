package DIV.gtcsolo.integration.jei.performance;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemHandlerHelper;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;

/**
 * An invocation-local index for Hyperlink's ordered ingredient aggregation.
 * Item identity only selects candidates; Forge still decides whether their tags and capabilities stack.
 */
public final class ItemStackAccumulator {
    private final List<ItemStack> target;
    private final BooleanSupplier stackIngredients;
    private final Map<Item, List<ItemStack>> candidates = new IdentityHashMap<>();

    public ItemStackAccumulator(List<ItemStack> target, BooleanSupplier stackIngredients) {
        this.target = target;
        this.stackIngredients = stackIngredients;
        for (ItemStack existing : target) bucket(existing).add(existing);
    }

    public void add(ItemStack incoming) {
        List<ItemStack> bucket = bucket(incoming);
        for (ItemStack existing : bucket) {
            if (ItemHandlerHelper.canItemStacksStack(existing, incoming)) {
                if (!stackIngredients.getAsBoolean()) return;
                existing.grow(incoming.getCount());
                return;
            }
        }
        if (!stackIngredients.getAsBoolean()) incoming.setCount(1);
        target.add(incoming);
        // Normalizing a zero-count stack may change getItem() from AIR back to its item.
        bucket(incoming).add(incoming);
    }

    private List<ItemStack> bucket(ItemStack stack) {
        return candidates.computeIfAbsent(stack.getItem(), ignored -> new ArrayList<>());
    }
}
