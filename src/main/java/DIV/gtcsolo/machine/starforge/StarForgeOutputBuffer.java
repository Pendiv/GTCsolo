package DIV.gtcsolo.machine.starforge;

import com.gregtechceu.gtceu.api.capability.recipe.IRecipeHandler;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableFluidTank;
import com.gregtechceu.gtceu.utils.OverlayedItemHandler;
import com.gregtechceu.gtceu.utils.OverlayedTankHandler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.wrapper.CombinedInvWrapper;

import java.util.ArrayList;
import java.util.List;

/** Holds an already-earned output batch until the real output hatches can accept it. */
public final class StarForgeOutputBuffer {
    private final List<ItemStack> items = new ArrayList<>();
    private final List<FluidStack> fluids = new ArrayList<>();
    private boolean changed;

    public void addItem(ItemStack stack) {
        if (stack != null && !stack.isEmpty()) { items.add(stack.copy()); changed = true; }
    }

    public void addFluid(FluidStack stack) {
        if (stack != null && !stack.isEmpty()) { fluids.add(stack.copy()); changed = true; }
    }

    public boolean isEmpty() {
        return items.isEmpty() && fluids.isEmpty();
    }

    public boolean takeChanged() {
        boolean result = changed;
        changed = false;
        return result;
    }

    /**
     * First reserve the entire batch in GT's overlays. If a real handler subsequently accepts
     * less than its preview, retain only that remainder; never re-emit an already delivered part.
     */
    public boolean flush(List<IItemHandlerModifiable> itemHandlers, List<NotifiableFluidTank> fluidHandlers) {
        if (isEmpty()) return true;
        var combined = new CombinedInvWrapper(itemHandlers.toArray(IItemHandlerModifiable[]::new));
        var itemPreview = new OverlayedItemHandler(combined);
        for (ItemStack stack : items) {
            if (itemPreview.insertStackedItemStack(stack, stack.getCount()) > 0) return false;
        }
        var orderedTanks = new ArrayList<>(fluidHandlers);
        orderedTanks.sort(IRecipeHandler.ENTRY_COMPARATOR);
        var fluidPreview = new OverlayedTankHandler(orderedTanks);
        for (FluidStack stack : fluids) {
            if (fluidPreview.tryFill(stack, stack.getAmount()) < stack.getAmount()) return false;
        }

        for (int index = 0; index < items.size();) {
            ItemStack remainder = items.get(index);
            for (int slot = 0; slot < combined.getSlots() && !remainder.isEmpty(); slot++) {
                int previousCount = remainder.getCount();
                remainder = combined.insertItem(slot, remainder, false);
                changed |= remainder.getCount() != previousCount;
                items.set(index, remainder);
            }
            if (remainder.isEmpty()) items.remove(index);
            else index++;
        }
        for (int index = 0; index < fluids.size();) {
            FluidStack remainder = fluids.get(index);
            for (NotifiableFluidTank tank : orderedTanks) {
                int filled = tank.fillInternal(remainder.copy(), IFluidHandler.FluidAction.EXECUTE);
                changed |= filled > 0;
                remainder.shrink(filled);
                if (remainder.isEmpty()) break;
            }
            if (remainder.isEmpty()) fluids.remove(index);
            else index++;
        }
        return isEmpty();
    }

    public CompoundTag save() {
        CompoundTag data = new CompoundTag();
        ListTag itemTags = new ListTag();
        for (ItemStack stack : items) {
            CompoundTag entry = new CompoundTag();
            ItemStack template = stack.copy();
            template.setCount(1); // Vanilla ItemStack NBT stores Count as a byte.
            entry.put("stack", template.save(new CompoundTag()));
            entry.putInt("count", stack.getCount());
            itemTags.add(entry);
        }
        data.put("items", itemTags);
        ListTag fluidTags = new ListTag();
        for (FluidStack stack : fluids) fluidTags.add(stack.writeToNBT(new CompoundTag()));
        data.put("fluids", fluidTags);
        return data;
    }

    public void load(CompoundTag data) {
        items.clear();
        fluids.clear();
        changed = false;
        ListTag itemTags = data.getList("items", Tag.TAG_COMPOUND);
        for (int i = 0; i < itemTags.size(); i++) {
            CompoundTag entry = itemTags.getCompound(i);
            ItemStack stack = ItemStack.of(entry.getCompound("stack"));
            if (!stack.isEmpty() && entry.getInt("count") > 0) {
                stack.setCount(entry.getInt("count"));
                items.add(stack);
            }
        }
        ListTag fluidTags = data.getList("fluids", Tag.TAG_COMPOUND);
        for (int i = 0; i < fluidTags.size(); i++) {
            FluidStack stack = FluidStack.loadFluidStackFromNBT(fluidTags.getCompound(i));
            if (!stack.isEmpty()) fluids.add(stack);
        }
    }
}
