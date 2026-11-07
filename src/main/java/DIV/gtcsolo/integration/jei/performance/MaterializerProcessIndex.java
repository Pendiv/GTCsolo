package DIV.gtcsolo.integration.jei.performance;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemHandlerHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/** Preserves the first matching process and output-count distinction of the native linear search. */
public final class MaterializerProcessIndex<P> {
    private final Function<P, ItemStack> output;
    private final Map<Item, Map<Integer, List<P>>> candidates = new IdentityHashMap<>();

    public MaterializerProcessIndex(Function<P, ItemStack> output) {
        this.output = output;
    }

    public P find(ItemStack result) {
        Map<Integer, List<P>> counts = candidates.get(result.getItem());
        if (counts == null) return null;
        List<P> bucket = counts.get(result.getCount());
        if (bucket == null) return null;
        for (P process : bucket) {
            ItemStack existing = output.apply(process);
            if (result.getCount() == existing.getCount()
                    && ItemHandlerHelper.canItemStacksStack(result, existing)) return process;
        }
        return null;
    }

    public void add(P process) {
        ItemStack result = output.apply(process);
        candidates.computeIfAbsent(result.getItem(), ignored -> new HashMap<>())
                .computeIfAbsent(result.getCount(), ignored -> new ArrayList<>()).add(process);
    }
}
