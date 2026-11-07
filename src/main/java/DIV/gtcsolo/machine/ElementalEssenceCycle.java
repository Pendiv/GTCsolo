package DIV.gtcsolo.machine;

import com.gregtechceu.gtceu.api.machine.trait.NotifiableItemStackHandler;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

/** A fixed pre-start draw, with actual stacks retained until delivery or cancellation recovery. */
public final class ElementalEssenceCycle {
    public static final List<ResourceLocation> ELEMENTS =
            List.of(
                    new ResourceLocation("mysticalagriculture", "air_essence"),
                    new ResourceLocation("mysticalagriculture", "earth_essence"),
                    new ResourceLocation("mysticalagriculture", "water_essence"),
                    new ResourceLocation("mysticalagriculture", "fire_essence"));
    private static final int[] WEIGHTS = {1, 2, 3, 4, 6, 4, 3, 2, 1, 1, 1};
    private List<ItemStack> planned, remaining;
    private final List<ItemStack> stored = new ArrayList<>();
    private int[] tribute = new int[4];
    private boolean active, collecting, returning, changed;

    public static int amountForRoll(int roll) {
        if (roll < 0 || roll >= 28)
            throw new IllegalArgumentException("Expected roll within 0..27");
        for (int amount = 0; amount < WEIGHTS.length; amount++) {
            roll -= WEIGHTS[amount];
            if (roll < 0) return amount;
        }
        throw new AssertionError();
    }

    public static int[] roll(RandomSource random) {
        int[] order = {0, 1, 2, 3}, amounts = new int[4];
        int types = 1 + random.nextInt(3);
        for (int i = 0; i < types; i++) {
            int selected = i + random.nextInt(4 - i);
            int swap = order[i];
            order[i] = order[selected];
            order[selected] = swap;
            amounts[order[i]] = amountForRoll(random.nextInt(28));
        }
        return amounts;
    }

    private static boolean matches(ItemStack stack, int element) {
        return !stack.isEmpty()
                && ELEMENTS.get(element).equals(ForgeRegistries.ITEMS.getKey(stack.getItem()));
    }

    private static ItemStack extract(IItemHandler source, int slot, int count, boolean simulate) {
        return source instanceof NotifiableItemStackHandler handler
                ? handler.extractItemInternal(slot, count, simulate)
                : source.extractItem(slot, count, simulate);
    }

    private static ItemStack insert(IItemHandler target, int slot, ItemStack stack) {
        return target instanceof NotifiableItemStackHandler handler
                ? handler.insertItemInternal(slot, stack, false)
                : target.insertItem(slot, stack, false);
    }

    private static List<ItemStack> sample(List<IItemHandler> inputs, int[] amounts) {
        var result = new ArrayList<ItemStack>();
        for (int element = 0; element < 4; element++) {
            int needed = amounts[element];
            for (var inventory : inputs) {
                for (int slot = 0; slot < inventory.getSlots() && needed > 0; slot++) {
                    if (!matches(inventory.getStackInSlot(slot), element)) continue;
                    var extracted = extract(inventory, slot, needed, true);
                    if (!matches(extracted, element)) continue;
                    int count = Math.min(needed, extracted.getCount());
                    needed -= count;
                    merge(result, extracted.copyWithCount(count));
                }
                if (needed == 0) break;
            }
            if (needed > 0) return null;
        }
        return result;
    }

    private static void merge(List<ItemStack> items, ItemStack stack) {
        for (var item : items)
            if (ItemStack.isSameItemSameTags(item, stack)) {
                item.grow(stack.getCount());
                return;
            }
        items.add(stack);
    }

    public static boolean hasRequiredEssences(List<IItemHandler> inputs) {
        return sample(inputs, new int[] {10, 10, 10, 10}) != null;
    }

    public boolean isIdle() {
        return planned == null && stored.isEmpty();
    }

    public boolean isActive() {
        return active;
    }

    public boolean isCollecting() {
        return collecting;
    }

    public boolean isReturning() {
        return returning;
    }

    public boolean hasDelivery() {
        return planned == null && !stored.isEmpty();
    }

    public List<ItemStack> storedItems() {
        return stored.stream().map(ItemStack::copy).toList();
    }

    public boolean takeChanged() {
        boolean result = changed;
        changed = false;
        return result;
    }

    public boolean ensurePlan(List<IItemHandler> inputs, RandomSource random) {
        if (planned != null) return true;
        if (!stored.isEmpty() || !hasRequiredEssences(inputs)) return false;
        int[] amounts = roll(random);
        var stacks = sample(inputs, amounts);
        if (stacks == null) return false;
        planned = stacks;
        remaining = new ArrayList<>(stacks.stream().map(ItemStack::copy).toList());
        tribute = new int[4];
        for (int i = 0; i < 4; i++) if (amounts[i] >= 2 && random.nextInt(4) == 0) tribute[i] = 1;
        changed = true;
        return true;
    }

    private void takeTribute(List<ItemStack> items) {
        for (int element = 0; element < 4; element++)
            if (tribute[element] > 0) {
                for (var stack : items)
                    if (matches(stack, element)) {
                        stack.shrink(1);
                        break;
                    }
            }
        items.removeIf(ItemStack::isEmpty);
    }

    public List<ItemStack> plannedOutputs() {
        if (planned == null) return List.of();
        var outputs = new ArrayList<>(planned.stream().map(ItemStack::copy).toList());
        takeTribute(outputs);
        return outputs;
    }

    public void begin() {
        if (planned == null || active)
            throw new IllegalStateException("No prepared elemental transfer");
        active = true;
        changed = true;
    }

    public boolean collect(List<IItemHandler> inputs) {
        if (!active || planned == null) return false;
        if (!collecting && !hasRequiredEssences(inputs)) return false;
        collecting = true;
        changed = true;
        for (var wanted : remaining) {
            for (var inventory : inputs) {
                for (int slot = 0; slot < inventory.getSlots() && !wanted.isEmpty(); slot++) {
                    if (!ItemStack.isSameItemSameTags(wanted, inventory.getStackInSlot(slot)))
                        continue;
                    var extracted = extract(inventory, slot, wanted.getCount(), false);
                    if (extracted.isEmpty()) continue;
                    merge(stored, extracted.copy());
                    if (ItemStack.isSameItemSameTags(wanted, extracted))
                        wanted.shrink(Math.min(wanted.getCount(), extracted.getCount()));
                }
                if (wanted.isEmpty()) break;
            }
        }
        if (remaining.stream().anyMatch(s -> !s.isEmpty())) return false;
        takeTribute(stored);
        planned = null;
        remaining = null;
        active = false;
        collecting = false;
        returning = false;
        return true;
    }

    public void abort() {
        if (planned == null) return;
        planned = null;
        remaining = null;
        active = false;
        collecting = false;
        returning = true;
        changed = true;
    }

    public void flush(List<IItemHandler> targets) {
        if (planned != null) return;
        var iterator = stored.listIterator();
        while (iterator.hasNext()) {
            ItemStack original = iterator.next(), rest = original.copy();
            for (var output : targets) {
                for (int slot = 0; slot < output.getSlots() && !rest.isEmpty(); slot++)
                    rest = returning ? output.insertItem(slot, rest, false) : insert(output, slot, rest);
                if (rest.isEmpty()) break;
            }
            if (rest.getCount() != original.getCount()) changed = true;
            if (rest.isEmpty()) iterator.remove();
            else iterator.set(rest);
        }
        if (stored.isEmpty() && returning) {
            returning = false;
            changed = true;
        }
    }

    private static ListTag writeItems(List<ItemStack> stacks) {
        var list = new ListTag();
        for (var stack : stacks) list.add(stack.save(new CompoundTag()));
        return list;
    }

    private static List<ItemStack> readItems(CompoundTag tag, String key) {
        var list = new ArrayList<ItemStack>();
        for (var entry : tag.getList(key, Tag.TAG_COMPOUND))
            list.add(ItemStack.of((CompoundTag) entry));
        return list;
    }

    public CompoundTag save() {
        var tag = new CompoundTag();
        tag.putBoolean("active", active);
        tag.putBoolean("collecting", collecting);
        tag.putBoolean("returning", returning);
        tag.putIntArray("tribute", tribute);
        if (planned != null) {
            tag.put("planned", writeItems(planned));
            tag.put("remaining", writeItems(remaining));
        }
        tag.put("stored", writeItems(stored));
        return tag;
    }

    public static ElementalEssenceCycle load(CompoundTag tag) {
        var cycle = new ElementalEssenceCycle();
        if (tag.contains("planned", Tag.TAG_LIST)) {
            cycle.planned = readItems(tag, "planned");
            cycle.remaining = readItems(tag, "remaining");
            cycle.active = tag.getBoolean("active");
            cycle.collecting = tag.getBoolean("collecting");
        }
        cycle.returning = tag.getBoolean("returning");
        int[] tribute = tag.getIntArray("tribute");
        if (tribute.length == 4)
            for (int i = 0; i < 4; i++) cycle.tribute[i] = tribute[i] == 1 ? 1 : 0;
        cycle.stored.addAll(readItems(tag, "stored"));
        return cycle;
    }
}
