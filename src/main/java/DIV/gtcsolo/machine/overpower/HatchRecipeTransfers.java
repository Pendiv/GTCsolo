package DIV.gtcsolo.machine.overpower;

import com.gregtechceu.gtceu.api.recipe.ingredient.FluidIngredient;
import com.gregtechceu.gtceu.api.recipe.ingredient.IntProviderIngredient;
import com.gregtechceu.gtceu.api.recipe.ingredient.SizedIngredient;
import com.gregtechceu.gtceu.api.transfer.fluid.CustomFluidTank;
import com.gregtechceu.gtceu.api.transfer.item.CustomItemStackHandler;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.items.IItemHandler;

/** Recipe-side quantities are independent of the one-unit phantom stacks displayed in the UI. */
public final class HatchRecipeTransfers {
    private HatchRecipeTransfers() {}

    public static List<Ingredient> creativeItems(List<Ingredient> left, IItemHandler phantoms, boolean input) {
        if (!input) { left.clear(); return null; }
        left.removeIf(ingredient -> {
            if (ingredient.isEmpty()) return true;
            for (int slot = 0; slot < phantoms.getSlots(); slot++) {
                ItemStack phantom = phantoms.getStackInSlot(slot);
                if (!phantom.isEmpty() && ingredient.test(phantom)) return true;
            }
            return false;
        });
        return left.isEmpty() ? null : left;
    }

    public static List<FluidIngredient> creativeFluids(List<FluidIngredient> left, CustomFluidTank[] phantoms, boolean input) {
        if (!input) { left.clear(); return null; }
        left.removeIf(ingredient -> {
            if (ingredient.isEmpty()) return true;
            for (CustomFluidTank tank : phantoms) {
                FluidStack phantom = tank.getFluid();
                if (!phantom.isEmpty() && ingredient.test(phantom)) return true;
            }
            return false;
        });
        return left.isEmpty() ? null : left;
    }

    public static List<Object> virtualItems(IItemHandler phantoms) {
        List<Object> result = new ArrayList<>();
        for (int slot = 0; slot < phantoms.getSlots(); slot++) {
            ItemStack phantom = phantoms.getStackInSlot(slot);
            if (!phantom.isEmpty()) result.add(phantom.copyWithCount(Integer.MAX_VALUE));
        }
        return result;
    }

    public static List<Object> virtualFluids(CustomFluidTank[] phantoms) {
        List<Object> result = new ArrayList<>();
        for (CustomFluidTank tank : phantoms) {
            FluidStack phantom = tank.getFluid();
            if (!phantom.isEmpty()) {
                FluidStack copy = phantom.copy();
                copy.setAmount(Integer.MAX_VALUE);
                result.add(copy);
            }
        }
        return result;
    }

    public static List<Ingredient> largeItemOutputs(List<Ingredient> left,
                                                    OverPowerItemHatchMachine.OverPowerItemStorage storage, boolean simulate) {
        CustomItemStackHandler working = storage;
        if (simulate) {
            var snapshot = new OverPowerItemHatchMachine.OverPowerItemStorage(storage.getSlots());
            snapshot.setFilter(storage.getFilter());
            for (int slot = 0; slot < storage.getSlots(); slot++)
                snapshot.setStackInSlot(slot, storage.getStackInSlot(slot).copy());
            working = snapshot;
        }
        for (var iterator = left.iterator(); iterator.hasNext();) {
            Ingredient ingredient = iterator.next();
            if (ingredient.isEmpty()) { iterator.remove(); continue; }
            if (ingredient instanceof IntProviderIngredient provider) {
                provider.setItemStacks(null);
                provider.setSampledCount(null);
            }
            ItemStack[] options = ingredient.getItems();
            if (options.length == 0 || options[0].isEmpty()) { iterator.remove(); continue; }
            int amount = ingredient instanceof SizedIngredient sized ? sized.getAmount() : options[0].getCount();
            ItemStack remainder = options[0].copyWithCount(amount);
            for (int pass = 0; pass < 2 && !remainder.isEmpty(); pass++) {
                for (int slot = 0; slot < working.getSlots() && !remainder.isEmpty(); slot++) {
                    boolean empty = working.getStackInSlot(slot).isEmpty();
                    if (pass == 0 && empty || pass == 1 && !empty) continue;
                    remainder = working.insertItem(slot, remainder, false);
                }
            }
            if (remainder.isEmpty()) iterator.remove();
            else if (ingredient instanceof SizedIngredient sized) sized.setAmount(remainder.getCount());
            else options[0].setCount(remainder.getCount());
        }
        return left.isEmpty() ? null : left;
    }

    /** Apply each request to one shared simulation, including duplicate fluid ingredients. */
    public static List<FluidIngredient> largeFluids(List<FluidIngredient> left, CustomFluidTank[] storages,
                                                   boolean input, boolean allowSameFluids, boolean simulate) {
        CustomFluidTank[] working = simulate ? storages.clone() : storages;
        boolean[] copied = simulate ? new boolean[storages.length] : null;
        for (var iterator = left.iterator(); iterator.hasNext();) {
            FluidIngredient ingredient = iterator.next();
            if (ingredient.isEmpty()) { iterator.remove(); continue; }
            FluidStack[] choices = ingredient.getStacks();
            if (choices.length == 0 || choices[0].isEmpty()) { iterator.remove(); continue; }
            int existing = -1;
            if (!input && !allowSameFluids) {
                for (int slot = 0; slot < working.length; slot++) {
                    if (!working[slot].getFluid().isEmpty() && working[slot].getFluid().isFluidEqual(choices[0])) {
                        existing = slot;
                        break;
                    }
                }
            }
            for (int slot = 0; slot < working.length && ingredient.getAmount() > 0; slot++) {
                if (existing >= 0 && slot != existing) continue;
                if (input && (working[slot].getFluid().isEmpty() || !ingredient.test(working[slot].getFluid()))) continue;
                if (!input && !working[slot].getFluid().isEmpty()
                        && !working[slot].getFluid().isFluidEqual(choices[0])) continue;
                if (simulate && !copied[slot]) {
                    CustomFluidTank source = storages[slot];
                    CustomFluidTank copy = new CustomFluidTank(source.getCapacity()) {
                        @Override public boolean isFluidValid(FluidStack stack) { return source.isFluidValid(stack); }
                    };
                    copy.setFluid(source.getFluid().copy());
                    working[slot] = copy;
                    copied[slot] = true;
                }
                int moved;
                if (input) moved = working[slot].drain(ingredient.getAmount(), FluidAction.EXECUTE).getAmount();
                else {
                    FluidStack output = choices[0].copy();
                    output.setAmount(ingredient.getAmount());
                    moved = working[slot].fill(output, FluidAction.EXECUTE);
                }
                ingredient.shrink(moved);
                if (!input && !allowSameFluids && (existing >= 0 || moved > 0)) break;
            }
            if (ingredient.getAmount() <= 0) iterator.remove();
        }
        return left.isEmpty() ? null : left;
    }
}
