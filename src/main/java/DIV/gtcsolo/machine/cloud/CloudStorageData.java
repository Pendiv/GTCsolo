package DIV.gtcsolo.machine.cloud;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;

import java.math.BigInteger;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/** World-wide storage shared by formed endpoints on the server thread. */
public final class CloudStorageData extends SavedData {

    private static final String DATA_NAME = "gtcsolo_cloud_storage";
    private static final int COLOR_COUNT = 16;
    public static final int UNCOLORED = -1;
    public static final int MAX_ITEM_SLOTS = 65_536;

    public enum Kind { ITEMS, FLUIDS }

    private record Endpoint(Kind kind, ResourceLocation dimension, long position) {}
    private record Contribution(int color, long amount) {}

    private static final class Capacity {
        private BigInteger total = BigInteger.ZERO;
        private long usable;

        void change(long delta, long limit) {
            total = total.add(BigInteger.valueOf(delta));
            usable = total.min(BigInteger.valueOf(limit)).longValue();
        }
    }

    private static final class ItemChannel {
        private final Capacity capacity = new Capacity();
        private final TreeMap<Integer, ItemStack> stacks = new TreeMap<>();
        private final BitSet occupied = new BitSet();

        void put(int slot, ItemStack stack) {
            stacks.put(slot, stack);
            occupied.set(slot);
        }

        void remove(int slot) {
            stacks.remove(slot);
            occupied.clear(slot);
        }
    }

    private static final class FluidChannel {
        private final Capacity capacity = new Capacity();
        private FluidStack fluid = FluidStack.EMPTY;
        private long amount;
    }

    private final ItemChannel[] items = new ItemChannel[COLOR_COUNT];
    private final FluidChannel[] fluids = new FluidChannel[COLOR_COUNT];
    private final Map<Endpoint, Contribution> endpoints = new HashMap<>();

    public CloudStorageData() {
        for (int color = 0; color < COLOR_COUNT; color++) {
            items[color] = new ItemChannel();
            fluids[color] = new FluidChannel();
        }
    }

    public static CloudStorageData get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage()
                .computeIfAbsent(CloudStorageData::load, CloudStorageData::new, DATA_NAME);
    }

    public static boolean validColor(int color) {
        return color >= 0 && color < COLOR_COUNT;
    }

    public long capacity(Kind kind, int color) {
        return validColor(color) ? capacityFor(kind, color).usable : 0;
    }

    /** Update this input endpoint's contribution. The contents stay in their original channels. */
    public void setCapacity(Kind kind, int color, ResourceLocation dimension, BlockPos pos, long capacity) {
        if (color != UNCOLORED && !validColor(color)) return;
        long amount = Math.min(capacity, capacityLimit(kind));
        Contribution contribution = validColor(color) && amount > 0 ? new Contribution(color, amount) : null;
        if (updateCapacity(new Endpoint(kind, dimension, pos.asLong()), contribution)) setDirty();
    }

    public void removeCapacity(Kind kind, ResourceLocation dimension, BlockPos pos) {
        if (updateCapacity(new Endpoint(kind, dimension, pos.asLong()), null)) setDirty();
    }

    private boolean updateCapacity(Endpoint endpoint, Contribution contribution) {
        Contribution old = endpoints.get(endpoint);
        if (Objects.equals(old, contribution)) return false;

        Kind kind = endpoint.kind();
        long limit = capacityLimit(kind);
        if (old != null) capacityFor(kind, old.color()).change(-old.amount(), limit);
        if (contribution == null) {
            endpoints.remove(endpoint);
        } else {
            endpoints.put(endpoint, contribution);
            capacityFor(kind, contribution.color()).change(contribution.amount(), limit);
        }
        return true;
    }

    private Capacity capacityFor(Kind kind, int color) {
        return kind == Kind.ITEMS ? items[color].capacity : fluids[color].capacity;
    }

    private static long capacityLimit(Kind kind) {
        return kind == Kind.ITEMS ? MAX_ITEM_SLOTS : Long.MAX_VALUE;
    }

    /** Highest usable slot plus one, including stored slots left over after a capacity reduction. */
    public int itemSlots(int color) {
        if (!validColor(color)) return 0;
        ItemChannel channel = items[color];
        int lastOccupied = channel.stacks.isEmpty() ? 0 : channel.stacks.lastKey() + 1;
        return Math.max(lastOccupied, (int) channel.capacity.usable);
    }

    public int usedItemSlots(int color) {
        return validColor(color) ? items[color].stacks.size() : 0;
    }

    public int nextItemSlot(int color, int start) {
        if (!validColor(color)) return -1;
        Integer slot = items[color].stacks.ceilingKey(Math.max(0, start));
        return slot == null ? -1 : slot;
    }

    public ItemStack itemInSlot(int color, int slot) {
        return itemReferenceForMenu(color, slot).copy();
    }

    /** Vanilla server menu transfer code can shrink this stack before notifying the slot. */
    ItemStack itemReferenceForMenu(int color, int slot) {
        if (!validColor(color)) return ItemStack.EMPTY;
        return items[color].stacks.getOrDefault(slot, ItemStack.EMPTY);
    }

    void itemChangedByMenu(int color, int slot) {
        if (!validColor(color)) return;
        ItemChannel channel = items[color];
        ItemStack stack = channel.stacks.get(slot);
        if (stack == null) return;
        if (stack.isEmpty()) channel.remove(slot);
        setDirty();
    }

    public ItemStack insertItem(int color, int slot, ItemStack stack, boolean simulate) {
        if (stack.isEmpty() || !isItemSlotWritable(color, slot)) return stack;
        ItemChannel channel = items[color];
        ItemStack current = channel.stacks.get(slot);
        int limit = stackLimit(stack);
        if (current != null) {
            if (!ItemStack.isSameItemSameTags(current, stack)) return stack;
            limit -= current.getCount();
        }
        int inserted = Math.min(limit, stack.getCount());
        if (inserted <= 0) return stack;
        if (!simulate) {
            if (current == null) channel.put(slot, stack.copyWithCount(inserted));
            else current.grow(inserted);
            setDirty();
        }
        return remainder(stack, stack.getCount() - inserted);
    }

    /** Fill compatible stacks, then allocate the first empty slots. */
    public ItemStack insertItem(int color, ItemStack stack, boolean simulate) {
        if (!validColor(color) || stack.isEmpty()) return stack;
        ItemChannel channel = items[color];
        int capacity = (int) channel.capacity.usable;
        int emptySlots = capacity - channel.stacks.size();
        if (capacity == 0 || emptySlots < 0) return stack;

        int stackLimit = stackLimit(stack);
        int remaining = stack.getCount();
        for (ItemStack current : channel.stacks.headMap(capacity, false).values()) {
            if (!ItemStack.isSameItemSameTags(current, stack)) continue;
            int inserted = Math.min(remaining, stackLimit - current.getCount());
            if (inserted <= 0) continue;
            if (!simulate) current.grow(inserted);
            remaining -= inserted;
            if (remaining == 0) break;
        }
        for (int slot = channel.occupied.nextClearBit(0); remaining > 0 && emptySlots > 0 && slot < capacity;
             slot = channel.occupied.nextClearBit(slot + 1)) {
            int inserted = Math.min(remaining, stackLimit);
            if (!simulate) channel.put(slot, stack.copyWithCount(inserted));
            remaining -= inserted;
            emptySlots--;
        }
        if (!simulate && remaining != stack.getCount()) setDirty();
        return remainder(stack, remaining);
    }

    public ItemStack extractItem(int color, int slot, int amount, boolean simulate) {
        if (!validColor(color) || amount <= 0) return ItemStack.EMPTY;
        ItemChannel channel = items[color];
        ItemStack current = channel.stacks.get(slot);
        if (current == null) return ItemStack.EMPTY;

        int extracted = Math.min(amount, current.getCount());
        ItemStack result = current.copyWithCount(extracted);
        if (!simulate) {
            if (extracted == current.getCount()) channel.remove(slot);
            else current.shrink(extracted);
            setDirty();
        }
        return result;
    }

    public boolean canSetItemInSlot(int color, int slot, ItemStack stack) {
        if (!validColor(color) || slot < 0 || slot >= MAX_ITEM_SLOTS) return false;
        ItemChannel channel = items[color];
        ItemStack current = channel.stacks.get(slot);
        long capacity = channel.capacity.usable;
        if (stack.isEmpty()) return current != null || slot < capacity;
        if (stack.getCount() > stackLimit(stack)) return false;
        if (current != null && ItemStack.isSameItemSameTags(current, stack)
                && stack.getCount() <= current.getCount()) return true;
        return isItemSlotWritable(color, slot);
    }

    boolean isItemSlotWritable(int color, int slot) {
        if (!validColor(color) || slot < 0) return false;
        ItemChannel channel = items[color];
        long capacity = channel.capacity.usable;
        int occupied = channel.stacks.size();
        return slot < capacity && (occupied < capacity || occupied == capacity && channel.occupied.get(slot));
    }

    public boolean setItemInSlot(int color, int slot, ItemStack stack) {
        if (!canSetItemInSlot(color, slot, stack)) return false;
        ItemChannel channel = items[color];
        ItemStack current = channel.stacks.getOrDefault(slot, ItemStack.EMPTY);
        if (ItemStack.matches(current, stack)) return true;
        if (stack.isEmpty()) channel.remove(slot);
        else channel.put(slot, stack.copy());
        setDirty();
        return true;
    }

    private static int stackLimit(ItemStack stack) {
        return Math.max(1, Math.min(64, stack.getMaxStackSize()));
    }

    private static ItemStack remainder(ItemStack stack, int remaining) {
        if (remaining <= 0) return ItemStack.EMPTY;
        return remaining == stack.getCount() ? stack : stack.copyWithCount(remaining);
    }

    public long fluidAmount(int color) {
        return validColor(color) ? fluids[color].amount : 0;
    }

    /** Forge stacks use int amounts. The channel's full amount is available via fluidAmount. */
    public FluidStack fluid(int color) {
        if (!validColor(color)) return FluidStack.EMPTY;
        FluidChannel channel = fluids[color];
        return channel.amount == 0 ? FluidStack.EMPTY
                : new FluidStack(channel.fluid, (int) Math.min(Integer.MAX_VALUE, channel.amount));
    }

    public int fill(int color, FluidStack stack, FluidAction action) {
        if (!validColor(color) || stack.isEmpty()) return 0;
        FluidChannel channel = fluids[color];
        if (channel.amount > 0 && !channel.fluid.isFluidEqual(stack)) return 0;

        long space = channel.capacity.usable - channel.amount;
        int filled = (int) Math.min(Math.max(0, space), stack.getAmount());
        if (filled > 0 && action.execute()) {
            if (channel.amount == 0) channel.fluid = new FluidStack(stack, 1);
            channel.amount += filled;
            setDirty();
        }
        return filled;
    }

    public FluidStack drain(int color, FluidStack stack, FluidAction action) {
        if (!validColor(color) || stack.isEmpty() || !fluids[color].fluid.isFluidEqual(stack)) return FluidStack.EMPTY;
        return drain(color, stack.getAmount(), action);
    }

    public FluidStack drain(int color, int amount, FluidAction action) {
        if (!validColor(color) || amount <= 0) return FluidStack.EMPTY;
        FluidChannel channel = fluids[color];
        int drained = (int) Math.min(channel.amount, amount);
        if (drained == 0) return FluidStack.EMPTY;

        FluidStack result = new FluidStack(channel.fluid, drained);
        if (action.execute()) {
            channel.amount -= drained;
            if (channel.amount == 0) channel.fluid = FluidStack.EMPTY;
            setDirty();
        }
        return result;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag savedEndpoints = new ListTag();
        endpoints.forEach((endpoint, contribution) -> {
            CompoundTag entry = new CompoundTag();
            entry.putString("Kind", endpoint.kind().name());
            entry.putString("Dimension", endpoint.dimension().toString());
            entry.putLong("Position", endpoint.position());
            entry.putInt("Color", contribution.color());
            entry.putLong("Capacity", contribution.amount());
            savedEndpoints.add(entry);
        });
        tag.put("Endpoints", savedEndpoints);

        ListTag savedItems = new ListTag();
        ListTag savedFluids = new ListTag();
        for (int color = 0; color < COLOR_COUNT; color++) {
            ItemChannel itemChannel = items[color];
            if (!itemChannel.stacks.isEmpty()) {
                CompoundTag entry = new CompoundTag();
                entry.putInt("Color", color);
                ListTag stacks = new ListTag();
                itemChannel.stacks.forEach((slot, stack) -> {
                    CompoundTag saved = stack.save(new CompoundTag());
                    saved.putInt("Slot", slot);
                    stacks.add(saved);
                });
                entry.put("Stacks", stacks);
                savedItems.add(entry);
            }
            FluidChannel fluidChannel = fluids[color];
            if (fluidChannel.amount > 0) {
                CompoundTag entry = new CompoundTag();
                entry.putInt("Color", color);
                entry.put("Fluid", fluidChannel.fluid.writeToNBT(new CompoundTag()));
                entry.putLong("Amount", fluidChannel.amount);
                savedFluids.add(entry);
            }
        }
        tag.put("Items", savedItems);
        tag.put("Fluids", savedFluids);
        return tag;
    }

    public static CloudStorageData load(CompoundTag tag) {
        CloudStorageData data = new CloudStorageData();
        for (Tag saved : tag.getList("Endpoints", Tag.TAG_COMPOUND)) {
            CompoundTag entry = (CompoundTag) saved;
            ResourceLocation dimension = ResourceLocation.tryParse(entry.getString("Dimension"));
            Kind kind = switch (entry.getString("Kind")) {
                case "ITEMS" -> Kind.ITEMS;
                case "FLUIDS" -> Kind.FLUIDS;
                default -> null;
            };
            int color = entry.getInt("Color");
            long capacity = entry.getLong("Capacity");
            if (dimension == null || kind == null || !validColor(color) || capacity <= 0) continue;
            data.updateCapacity(new Endpoint(kind, dimension, entry.getLong("Position")),
                    new Contribution(color, Math.min(capacity, capacityLimit(kind))));
        }
        for (Tag saved : tag.getList("Items", Tag.TAG_COMPOUND)) {
            CompoundTag entry = (CompoundTag) saved;
            int color = entry.getInt("Color");
            if (!validColor(color)) continue;
            for (Tag stackTag : entry.getList("Stacks", Tag.TAG_COMPOUND)) {
                CompoundTag savedStack = (CompoundTag) stackTag;
                int slot = savedStack.getInt("Slot");
                if (slot < 0 || slot >= MAX_ITEM_SLOTS) continue;
                ItemStack stack = ItemStack.of(savedStack);
                if (!stack.isEmpty()) {
                    stack.setCount(Math.min(stack.getCount(), stackLimit(stack)));
                    data.items[color].put(slot, stack);
                }
            }
        }
        for (Tag saved : tag.getList("Fluids", Tag.TAG_COMPOUND)) {
            CompoundTag entry = (CompoundTag) saved;
            int color = entry.getInt("Color");
            long amount = entry.getLong("Amount");
            if (!validColor(color) || amount <= 0) continue;
            FluidStack fluid = FluidStack.loadFluidStackFromNBT(entry.getCompound("Fluid"));
            if (fluid.isEmpty()) continue;
            fluid.setAmount(1);
            data.fluids[color].fluid = fluid;
            data.fluids[color].amount = amount;
        }
        return data;
    }
}
