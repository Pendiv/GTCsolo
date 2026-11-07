package DIV.gtcsolo.machine.overpower;

import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.capability.recipe.IRecipeCapabilityHolder;
import com.gregtechceu.gtceu.api.capability.recipe.IRecipeHandler;
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

/** Scoped replacement for GT's normal-stack overlay only when one of our special output hatches is present. */
public final class HatchParallelSupport {
    private HatchParallelSupport() {}

    public static @Nullable Integer itemLimit(GTRecipe recipe, IRecipeCapabilityHolder holder, int multiplier) {
        if (holder instanceof ItemRecipeCapability.ICustomParallel) return null;
        var handlers = holder.getCapabilitiesProxy().get(IO.OUT, ItemRecipeCapability.CAP);
        if (handlers == null) return null;
        List<IRecipeHandler<?>> ordered = new ArrayList<>(handlers);
        ordered.sort(IRecipeHandler.ENTRY_COMPARATOR);
        boolean large = false;
        List<IItemHandlerModifiable> outputs = new ArrayList<>();
        for (var handler : ordered) {
            if (handler instanceof CreativeItemHatchMachine.CreativeItemRecipeHandler creative && creative.handlerIO == IO.OUT)
                return multiplier;
            if (handler instanceof OverPowerItemHatchMachine.LargeItemRecipeHandler) large = true;
            if (handler instanceof IItemHandlerModifiable inventory) outputs.add(inventory);
        }
        if (!large) return null;
        List<ItemStack> items = new ArrayList<>();
        for (var content : recipe.getOutputContents(ItemRecipeCapability.CAP)) {
            var ingredient = ItemRecipeCapability.CAP.of(content.getContent());
            if (ingredient.isEmpty()) continue;
            ItemStack[] alternatives = ingredient.getItems();
            if (alternatives.length == 0 || alternatives[0].isEmpty()) continue;
            items.add(alternatives[0].copy());
        }
        return maxItemMultiplier(outputs, items, multiplier);
    }

    public static boolean hasCreativeFluidOutput(IRecipeCapabilityHolder holder) {
        if (holder instanceof FluidRecipeCapability.ICustomParallel) return false;
        var handlers = holder.getCapabilitiesProxy().get(IO.OUT, FluidRecipeCapability.CAP);
        if (handlers == null) return false;
        return handlers.stream().anyMatch(handler ->
                handler instanceof CreativeFluidHatchMachine.CreativeFluidRecipeHandler creative && creative.handlerIO == IO.OUT);
    }

    public static int maxItemMultiplier(List<? extends IItemHandler> handlers, List<ItemStack> outputs, int maximum) {
        int low = 0, high = Math.max(0, maximum);
        while (low < high) {
            int middle = (int) (low + ((long) high - low + 1) / 2);
            if (fits(handlers, outputs, middle)) low = middle;
            else high = middle - 1;
        }
        return low;
    }

    private static boolean fits(List<? extends IItemHandler> handlers, List<ItemStack> outputs, int multiplier) {
        List<ItemStack> remainders = new ArrayList<>(outputs.size());
        for (ItemStack output : outputs) {
            if (output.isEmpty()) continue;
            int amount = (int) Math.min(Integer.MAX_VALUE, (long) output.getCount() * multiplier);
            remainders.add(output.copyWithCount(amount));
        }
        // Recipe handlers each receive the entire remaining output list, in handler order.
        // A global "merge all hatches first" pass could promise space execution cannot use.
        for (IItemHandler handler : handlers) {
            OutputSnapshot snapshot = new OutputSnapshot(handler);
            for (int outputIndex = 0; outputIndex < remainders.size(); outputIndex++) {
                ItemStack left = remainders.get(outputIndex);
                for (int pass = 0; pass < (snapshot.large ? 2 : 1) && !left.isEmpty(); pass++) {
                    for (int slot = 0; slot < snapshot.getSlots() && !left.isEmpty(); slot++) {
                        boolean empty = snapshot.getStackInSlot(slot).isEmpty();
                        if (snapshot.large && (pass == 0 && empty || pass == 1 && !empty)) continue;
                        left = snapshot.insertItem(slot, left, false);
                    }
                }
                remainders.set(outputIndex, left);
            }
        }
        return remainders.stream().allMatch(ItemStack::isEmpty);
    }

    /** Same normal-slot capacity/filter rules as GT's overlay, with explicit large-hatch capacity. */
    private static final class OutputSnapshot extends ItemStackHandler {
        private final IItemHandler source;
        private final boolean large;

        OutputSnapshot(IItemHandler source) {
            super(source.getSlots());
            this.source = source;
            this.large = source instanceof OverPowerItemHatchMachine.LargeItemRecipeHandler
                    || source instanceof OverPowerItemHatchMachine.OverPowerItemStorage;
            for (int slot = 0; slot < getSlots(); slot++) stacks.set(slot, source.getStackInSlot(slot).copy());
        }

        @Override public int getSlotLimit(int slot) { return source.getSlotLimit(slot); }
        @Override public boolean isItemValid(int slot, ItemStack stack) { return source.isItemValid(slot, stack); }
        @Override protected int getStackLimit(int slot, ItemStack stack) {
            return large ? getSlotLimit(slot) : Math.min(getSlotLimit(slot), stack.getMaxStackSize());
        }
    }
}
