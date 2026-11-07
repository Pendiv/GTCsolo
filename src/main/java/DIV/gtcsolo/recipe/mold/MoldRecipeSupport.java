package DIV.gtcsolo.recipe.mold;

import DIV.gtcsolo.item.IronCastingMoldItem;
import DIV.gtcsolo.registry.IronCastingMolds;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.capability.recipe.IRecipeCapabilityHolder;
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableItemStackHandler;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.content.Content;
import com.gregtechceu.gtceu.api.recipe.content.ContentModifier;
import com.gregtechceu.gtceu.api.recipe.ingredient.SizedIngredient;
import com.gregtechceu.gtceu.api.transfer.item.CustomItemStackHandler;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class MoldRecipeSupport {
    private MoldRecipeSupport() {}

    public static @Nullable MoldIngredient mold(Ingredient ingredient) {
        while (ingredient instanceof SizedIngredient sized) ingredient = sized.getInner();
        return ingredient instanceof MoldIngredient mold ? mold : null;
    }

    /** Copy only changed input entries. Constructor copies and network/reload round trips are idempotent. */
    public static @Nullable Map<RecipeCapability<?>, List<Content>> extend(Map<RecipeCapability<?>, List<Content>> inputs) {
        if (inputs.isEmpty()) return null;
        var items = inputs.get(ItemRecipeCapability.CAP);
        if (items == null) return null;
        List<Content> changed = null;
        for (int index = 0; index < items.size(); index++) {
            Content content = items.get(index);
            if (content.chance != 0 || !(content.content instanceof Ingredient ingredient)) continue;
            Ingredient inner = ingredient instanceof SizedIngredient sized ? sized.getInner() : ingredient;
            if (!inner.isSimple()) continue;
            ItemStack[] candidates = inner.getItems();
            if (candidates.length != 1 || candidates[0].isEmpty()) continue;
            String shape = IronCastingMolds.shapeOf(candidates[0].getItem());
            if (shape == null) continue;
            var iron = new ItemStack(IronCastingMolds.MOLDS.get(shape).get());
            // Consumed/recycled molds are materials, not reusable tools; leave those recipes intact.
            boolean overlaps = items.stream().anyMatch(other -> other.chance != 0 && other.content instanceof Ingredient otherInput
                    && (otherInput.test(candidates[0]) || otherInput.test(iron)));
            if (overlaps) continue;
            int count = ingredient instanceof SizedIngredient sized ? sized.getAmount() : candidates[0].getCount();
            if (count < 1) continue;
            if (changed == null) changed = new ArrayList<>(items);
            changed.set(index, new Content(SizedIngredient.create(new MoldIngredient(shape, count), count),
                    0, content.maxChance, 0, content.slotName, content.uiName));
        }
        if (changed == null) return null;
        var result = new HashMap<>(inputs);
        result.put(ItemRecipeCapability.CAP, changed);
        return result;
    }

    public static boolean hasMolds(GTRecipe recipe) {
        return recipe.getInputContents(ItemRecipeCapability.CAP).stream()
                .anyMatch(content -> content.content instanceof Ingredient ingredient && mold(ingredient) != null);
    }

    /** Native input simulation also handles distinct buses, slot names and normal materials. */
    public static int maxParallel(IRecipeCapabilityHolder holder, GTRecipe recipe, int limit) {
        int low = 0, high = Math.max(0, limit);
        while (low < high) {
            int candidateCount = (int) (((long) low + high + 1) / 2);
            long total = (long) Math.max(1, recipe.parallels) * candidateCount;
            boolean matches = false;
            if (total <= Integer.MAX_VALUE) {
                GTRecipe candidate = recipe.copy(ContentModifier.multiplier(candidateCount), false);
                candidate.parallels = (int) total;
                matches = candidate.matchRecipeContents(IO.IN, holder,
                        Map.of(ItemRecipeCapability.CAP, candidate.getInputContents(ItemRecipeCapability.CAP)), false).isSuccess();
            }
            if (matches) low = candidateCount;
            else high = candidateCount - 1;
        }
        return low;
    }

    /** Called only for input lists containing our marker. Native handling retains all other input behavior. */
    public static @Nullable List<Ingredient> handle(GTRecipe recipe, List<Ingredient> left,
                                                   CustomItemStackHandler storage, boolean simulate) {
        List<Ingredient> molds = new ArrayList<>();
        left.removeIf(ingredient -> {
            if (mold(ingredient) == null) return false;
            molds.add(ingredient);
            return true;
        });
        // The recursive call contains no molds and therefore goes directly through the original handler.
        NotifiableItemStackHandler.handleRecipe(IO.IN, recipe, left, simulate, IO.IN, storage);
        long[] used = new long[storage.getSlots()];
        for (Ingredient input : molds) {
            MoldIngredient mold = mold(input);
            long required = mold.pending && input instanceof SizedIngredient sized ? sized.getAmount()
                    : (long) mold.toolsPerRecipe * Math.max(1, recipe.parallels);
            if (required > Integer.MAX_VALUE) { left.add(input); continue; }
            long remaining = required;
            // Prefer the permanent mold in the same inventory; iron tools stay available as a fallback.
            for (int pass = 0; pass < 2 && remaining > 0; pass++) {
                for (int slot = 0; slot < storage.getSlots() && remaining > 0; slot++) {
                    ItemStack stored = storage.getStackInSlot(slot);
                    boolean permanent = stored.is(mold.original);
                    if (stored.isEmpty() || (pass == 0) != permanent || !mold.test(stored)) continue;
                    long capacity = permanent ? (long) stored.getCount() * Math.max(1, recipe.parallels)
                            : IronCastingMoldItem.remaining(stored);
                    long available = simulate || permanent ? capacity - used[slot] : capacity;
                    int taken = (int) Math.min(remaining, Math.max(0, available));
                    if (taken == 0) continue;
                    if (simulate || permanent) used[slot] += taken;
                    if (!simulate && !permanent) {
                        ItemStack changed = stored.copy();
                        changed.setDamageValue(changed.getDamageValue() + taken);
                        storage.setStackInSlot(slot, changed.getDamageValue() >= IronCastingMoldItem.USES ? ItemStack.EMPTY : changed);
                    }
                    remaining -= taken;
                }
            }
            if (remaining > 0) left.add(SizedIngredient.create(mold.pending(), (int) remaining));
        }
        return left.isEmpty() ? null : left;
    }
}
