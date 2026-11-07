package DIV.gtcsolo.machine;

import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.content.Content;
import com.gregtechceu.gtceu.api.recipe.ingredient.IntProviderIngredient;
import com.gregtechceu.gtceu.api.recipe.ingredient.SizedIngredient;
import com.gregtechceu.gtceu.api.transfer.fluid.CustomFluidTank;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.items.IItemHandlerModifiable;

import java.util.*;

/** Stages all base/extra materials in copies, then commits only if every source still matches. */
public final class IndustrialCircuitInputs {
    private IndustrialCircuitInputs() {}

    private static boolean infinite(Object storage) {
        return storage instanceof DIV.gtcsolo.api.transfer.InfiniteInputSupply source && source.isInfiniteInput();
    }

    public record Bus(IItemHandlerModifiable items, IItemHandlerModifiable circuit) {}

    public static Plan extraPlan(GTRecipe recipe, Bus bus, List<CustomFluidTank> fluids) {
        Plan plan = new Plan();
        return plan.takeItems(recipe, bus) && plan.takeFluids(recipe, fluids) ? plan : null;
    }

    public static Plan plan(GTRecipe recipe, List<Bus> base, List<Bus> extra,
                            List<CustomFluidTank> fluids, int repeats) {
        if (repeats < 0 || repeats > 6) throw new IllegalArgumentException("Circuit line supports 0..6 repeats");
        Plan plan = new Plan();
        if (!plan.takeBase(recipe, base) || !plan.takeFluids(recipe, fluids)) return null;
        Set<IItemHandlerModifiable> unique = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Bus bus : base) unique.add(bus.items());
        for (Bus bus : extra) {
            if (!unique.add(bus.items())) continue;
            if (plan.multiplier > repeats) break;
            Plan candidate = plan.copy();
            if (candidate.takeItems(recipe, bus) && candidate.takeFluids(recipe, fluids)) {
                candidate.multiplier++;
                plan = candidate;
            }
        }
        return plan;
    }

    public static final class Plan {
        private final Map<IItemHandlerModifiable, ItemStack[]> before = new IdentityHashMap<>();
        private final Map<IItemHandlerModifiable, ItemStack[]> after = new IdentityHashMap<>();
        private final Map<CustomFluidTank, FluidStack> fluidBefore = new IdentityHashMap<>();
        private final Map<CustomFluidTank, FluidStack> fluidAfter = new IdentityHashMap<>();
        private int multiplier = 1;
        private boolean committed;

        public int multiplier() { return multiplier; }

        private static ItemStack[] stacks(IItemHandlerModifiable inventory) {
            ItemStack[] result = new ItemStack[inventory.getSlots()];
            for (int slot = 0; slot < result.length; slot++) result[slot] = inventory.getStackInSlot(slot).copy();
            return result;
        }

        private ItemStack[] working(IItemHandlerModifiable inventory) {
            if (inventory == null) return new ItemStack[0];
            before.computeIfAbsent(inventory, Plan::stacks);
            return after.computeIfAbsent(inventory, ignored -> {
                ItemStack[] contents = stacks(inventory);
                if (infinite(inventory)) for (ItemStack stack : contents) if (!stack.isEmpty()) stack.setCount(Integer.MAX_VALUE);
                return contents;
            });
        }

        private Plan copy() {
            Plan copy = new Plan();
            copy.multiplier = multiplier;
            before.forEach((inventory, values) -> copy.before.put(inventory, copyStacks(values)));
            after.forEach((inventory, values) -> copy.after.put(inventory, copyStacks(values)));
            fluidBefore.forEach((tank, value) -> copy.fluidBefore.put(tank, value.copy()));
            fluidAfter.forEach((tank, value) -> copy.fluidAfter.put(tank, value.copy()));
            return copy;
        }

        private static ItemStack[] copyStacks(ItemStack[] stacks) {
            return Arrays.stream(stacks).map(ItemStack::copy).toArray(ItemStack[]::new);
        }

        private boolean takeBase(GTRecipe recipe, List<Bus> buses) {
            int index = 0;
            for (Content content : recipe.getInputContents(ItemRecipeCapability.CAP)) {
                Ingredient ingredient = ItemRecipeCapability.CAP.of(content.content);
                int amount = amount(ingredient);
                if (amount < 1 || !deterministic(content, ingredient)) return false;
                if (content.chance == 0) {
                    if (!take(buses, ingredient, amount, false)) return false;
                } else {
                    if (index >= buses.size()) return false;
                    Bus bus = buses.get(index++);
                    ItemStack[] items = working(bus.items());
                    if (items.length != 1 || !ingredient.test(items[0]) || items[0].getCount() < amount) return false;
                    if (!infinite(bus.items())) items[0].shrink(amount);
                }
            }
            return true;
        }

        private boolean takeItems(GTRecipe recipe, Bus bus) {
            for (Content content : recipe.getInputContents(ItemRecipeCapability.CAP)) {
                Ingredient ingredient = ItemRecipeCapability.CAP.of(content.content);
                int amount = amount(ingredient);
                if (amount < 1 || !deterministic(content, ingredient)
                        || !take(List.of(bus), ingredient, amount, content.chance != 0)) return false;
            }
            return true;
        }

        private boolean take(List<Bus> buses, Ingredient ingredient, int amount, boolean consume) {
            Set<IItemHandlerModifiable> visited = Collections.newSetFromMap(new IdentityHashMap<>());
            for (Bus bus : buses) for (IItemHandlerModifiable inventory : new IItemHandlerModifiable[]{bus.items(), bus.circuit()}) {
                if (inventory == null || !visited.add(inventory)) continue;
                for (ItemStack stack : working(inventory)) {
                    if (!ingredient.test(stack)) continue;
                    int used = Math.min(amount, stack.getCount());
                    if (consume && !infinite(inventory)) stack.shrink(used);
                    amount -= used;
                    if (amount == 0) return true;
                }
            }
            return false;
        }

        private boolean takeFluids(GTRecipe recipe, List<CustomFluidTank> tanks) {
            for (Content content : recipe.getInputContents(FluidRecipeCapability.CAP)) {
                if (content.slotName != null || content.maxChance <= 0 || content.chance < 0
                        || content.chance != 0 && content.chance < content.maxChance) return false;
                var ingredient = FluidRecipeCapability.CAP.of(content.content);
                int amount = ingredient.getAmount();
                if (amount <= 0) return false;
                Set<CustomFluidTank> visited = Collections.newSetFromMap(new IdentityHashMap<>());
                for (CustomFluidTank tank : tanks) {
                    if (!visited.add(tank)) continue;
                    fluidBefore.computeIfAbsent(tank, ignored -> tank.getFluid().copy());
                    FluidStack stack = fluidAfter.computeIfAbsent(tank, ignored -> {
                        FluidStack available = tank.getFluid().copy();
                        if (infinite(tank) && !available.isEmpty()) available.setAmount(Integer.MAX_VALUE);
                        return available;
                    });
                    if (!ingredient.test(stack)) continue;
                    int used = Math.min(amount, stack.getAmount());
                    if (content.chance != 0 && !infinite(tank)) stack.shrink(used);
                    amount -= used;
                    if (amount == 0) break;
                }
                if (amount > 0) return false;
            }
            return true;
        }

        private static boolean deterministic(Content content, Ingredient ingredient) {
            return content.slotName == null && content.maxChance > 0 && content.chance >= 0
                    && !(ingredient instanceof IntProviderIngredient)
                    && (content.chance == 0 || content.chance >= content.maxChance);
        }

        private static int amount(Ingredient ingredient) {
            if (ingredient instanceof SizedIngredient sized) return sized.getAmount();
            ItemStack[] choices = ingredient.getItems();
            return choices.length == 0 ? 0 : choices[0].getCount();
        }

        public boolean commit() {
            if (committed) return false;
            for (var entry : before.entrySet()) {
                if (entry.getKey().getSlots() != entry.getValue().length) return false;
                for (int slot = 0; slot < entry.getValue().length; slot++) {
                    if (!ItemStack.matches(entry.getValue()[slot], entry.getKey().getStackInSlot(slot))) return false;
                }
            }
            for (var entry : fluidBefore.entrySet()) {
                if (!entry.getValue().isFluidStackIdentical(entry.getKey().getFluid())) return false;
            }
            committed = true;
            after.forEach((inventory, stacks) -> {
                if (infinite(inventory)) return;
                for (int slot = 0; slot < stacks.length; slot++) {
                    if (!ItemStack.matches(before.get(inventory)[slot], stacks[slot])) inventory.setStackInSlot(slot, stacks[slot].copy());
                }
            });
            fluidAfter.forEach((tank, stack) -> {
                if (infinite(tank)) return;
                if (!fluidBefore.get(tank).isFluidStackIdentical(stack)) tank.setFluid(stack.copy());
            });
            return true;
        }
    }
}
