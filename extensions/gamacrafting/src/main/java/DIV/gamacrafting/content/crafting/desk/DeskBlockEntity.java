package DIV.gamacrafting.content.crafting.desk;

import DIV.gamacrafting.Gamacrafting;
import DIV.gamacrafting.api.content.IOItemHandler;
import it.unimi.dsi.fastutil.ints.IntAVLTreeSet;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.wrapper.RecipeWrapper;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

public class DeskBlockEntity extends BlockEntity implements MenuProvider {
    public static final int GRID_WIDTH = 9;
    public static final int GRID_SIZE = GRID_WIDTH * GRID_WIDTH;
    private final Object2ObjectOpenHashMap<Item, IntAVLTreeSet> lock = new Object2ObjectOpenHashMap<>();
    private final Object2IntOpenHashMap<Item> minimum = new Object2IntOpenHashMap<>();
    private boolean consumingRecipe;
    private ItemStack completedOutput = ItemStack.EMPTY;

    public final ItemStackHandler inventory = new ItemStackHandler(GRID_SIZE) {

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return DeskBlockEntity.this.checkLock(stack.getItem(), slot);
        }

        @Override
        public void setStackInSlot(int slot, @NotNull ItemStack stack) {
            if (DeskBlockEntity.this.checkLock(stack.getItem(), slot)) {
                super.setStackInSlot(slot, stack);
            }
        }

        @NotNull
        @Override
        public ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            if (DeskBlockEntity.this.checkLockMinimum(stack.getItem(), slot)) {
                return super.insertItem(slot, stack, simulate);
            } else {
                return stack;
            }
        }

        @Override
        protected void onContentsChanged(int slot) {
            if (!DeskBlockEntity.this.consumingRecipe) {
                DeskBlockEntity.this.inventoryChanged();
            }
        }
    };
    public final ItemStackHandler result = new ItemStackHandler() {
        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return false;
        }

        @NotNull
        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            DeskBlockEntity.this.updateRecipe();
            ItemStack output = this.getStackInSlot(slot);
            if (DeskBlockEntity.this.minecraft || output.isEmpty() || amount <= 0) {
                return ItemStack.EMPTY;
            }

            if (simulate || !(DeskBlockEntity.this.getLevel() instanceof ServerLevel)) {
                return super.extractItem(slot, amount, simulate);
            }

            ItemStack taken = output.copyWithCount(Math.min(output.getCount(), Math.min(amount, output.getMaxStackSize())));
            boolean alreadyCrafted = !DeskBlockEntity.this.completedOutput.isEmpty();
            DeskBlockEntity.this.completedOutput = output.copyWithCount(output.getCount() - taken.getCount());
            if (alreadyCrafted) {
                DeskBlockEntity.this.updateRecipe();
                DeskBlockEntity.this.setChanged();
            } else {
                DeskBlockEntity.this.consumeRecipe();
            }
            return taken;
        }
    };
    private final RecipeWrapper wrapper = new RecipeWrapper(this.inventory);
    private final IOItemHandler<ItemStackHandler> ioHandler = new IOItemHandler<>(this.inventory, this.result);
    private LazyOptional<IItemHandler> capability = LazyOptional.of(() -> this.ioHandler);
    private boolean minecraft = false;
    public Player minecrafter = null;
    public final ContainerData data = new ContainerData() {
        @Override
        public int get(int pIndex) {
            return DeskBlockEntity.this.minecraft ? 1 : 0;
        }

        @Override
        public void set(int pIndex, int pValue) {
            DeskBlockEntity.this.minecraft = pValue > 0;
        }

        @Override
        public int getCount() {
            return 1;
        }
    };

    public DeskBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(Gamacrafting.DESK_BLOCK_ENTITY.get(), pPos, pBlockState);
    }

    public void updateRecipe() {
        if (this.getLevel() instanceof ServerLevel serverLevel) {
            if (!this.completedOutput.isEmpty()) {
                this.minecraft = false;
                this.result.setStackInSlot(0, this.completedOutput.copy());
                return;
            }
            serverLevel.getRecipeManager()
                    .getRecipeFor(Gamacrafting.DESK_TYPE.get(), this.wrapper, serverLevel)
                    .ifPresentOrElse(recipe -> {
                        ItemStack output = recipe.assemble(this.wrapper, serverLevel.registryAccess());
                        this.minecraft = recipe.isMinecraft() && !output.isEmpty();
                        this.result.setStackInSlot(0, output);
                    }, () -> {
                        this.minecraft = false;
                        this.result.setStackInSlot(0, ItemStack.EMPTY);
                    });
        }
    }

    void consumeRecipe() {
        this.consumingRecipe = true;
        try {
            for (int index = 0; index < this.inventory.getSlots(); index++) {
                this.inventory.extractItem(index, 1, false);
            }
        } finally {
            this.consumingRecipe = false;
            this.inventoryChanged();
        }
    }

    void inventoryChanged() {
        this.updateRecipe();
        this.updateLockMinimum();
        this.setChanged();
    }

    public void dropOrMinecraft() {
        if (this.getLevel() instanceof ServerLevel serverLevel) {
            this.updateRecipe();
            if (this.minecraft) {
                List<ItemStack> ingredients = new ArrayList<>();
                for (int index = 0; index < this.inventory.getSlots(); index++) {
                    ItemStack ingredient = this.inventory.getStackInSlot(index);
                    if (!ingredient.isEmpty()) {
                        ingredients.add(ingredient);
                    }
                }

                DeskHandler.minecraftAt(serverLevel, this.getBlockPos(), ingredients, this.result.getStackInSlot(0));
                this.consumeRecipe();
            }

            if (this.minecrafter == null) {
                Containers.dropContents(serverLevel, this.getBlockPos(), this.wrapper);
                Containers.dropItemStack(serverLevel, this.worldPosition.getX(), this.worldPosition.getY(),
                        this.worldPosition.getZ(), this.completedOutput);
            } else {
                for (int index = 0; index < this.inventory.getSlots(); index++) {
                    DeskBlock.give(this.minecrafter, this.inventory.getStackInSlot(index));
                }
                DeskBlock.give(this.minecrafter, this.completedOutput);
            }
            this.completedOutput = ItemStack.EMPTY;
        }
    }

    public boolean isMinecraft() {
        return this.minecraft;
    }

    public void lockRecipe(Object2ObjectOpenHashMap<Item, IntAVLTreeSet> lock) {
        this.readLock(lock);
        this.setChanged();
        if (this.level instanceof ServerLevel serverLevel) {
            BlockState state = this.getBlockState();
            serverLevel.sendBlockUpdated(this.worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }

    public void lockCurrentRecipe() {
        Object2ObjectOpenHashMap<Item, IntAVLTreeSet> slots = new Object2ObjectOpenHashMap<>();
        for (int index = 0; index < this.inventory.getSlots(); index++) {
            ItemStack stack = this.inventory.getStackInSlot(index);
            if (!stack.isEmpty()) {
                slots.computeIfAbsent(stack.getItem(), item -> new IntAVLTreeSet()).add(index);
            }
        }
        this.lockRecipe(slots);
    }

    public void clearLock() {
        this.lockRecipe(new Object2ObjectOpenHashMap<>());
    }

    private void readLock(Object2ObjectOpenHashMap<Item, IntAVLTreeSet> lock) {
        this.lock.clear();
        lock.forEach((item, indexes) -> {
            if (item != null && item != Items.AIR) {
                IntAVLTreeSet valid = new IntAVLTreeSet();
                for (IntIterator iterator = indexes.iterator(); iterator.hasNext();) {
                    int index = iterator.nextInt();
                    if (index >= 0 && index < GRID_SIZE) {
                        valid.add(index);
                    }
                }
                if (!valid.isEmpty()) {
                    this.lock.put(item, valid);
                }
            }
        });
        this.updateLockMinimum();
    }

    public void foeEachLocked(BiConsumer<Item, IntAVLTreeSet> consumer) {
        this.lock.forEach(consumer);
    }

    public boolean checkLock(Item item, int index) {
        return item == Items.AIR || this.lock.isEmpty() || (this.lock.containsKey(item) && this.lock.get(item).contains(index));
    }

    public boolean checkLockMinimum(Item item, int index) {
        return item == Items.AIR || this.minimum.isEmpty() || (this.minimum.containsKey(item) && this.minimum.getInt(item) == index);
    }

    private void updateLockMinimum() {
        this.minimum.clear();

        this.lock.forEach((item, set) -> {
            int count = -1;
            int slot = set.firstInt();
            IntIterator iterator = set.iterator();
            while (iterator.hasNext()) {
                int index = iterator.nextInt();
                ItemStack stack = this.inventory.getStackInSlot(index);
                if (stack.isEmpty()) {
                    slot = index;
                    break;
                }
                int current = stack.getCount();
                if (count == -1 || count > current) {
                    count = current;
                    slot = index;
                }
            }
            this.minimum.put(item, slot);
        });
    }

    @Override
    public void onLoad() {
        this.updateRecipe();
        super.onLoad();
    }

    @NotNull
    @Override
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        return cap == ForgeCapabilities.ITEM_HANDLER ? this.capability.cast() : super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        this.capability.invalidate();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        this.capability = LazyOptional.of(() -> this.ioHandler);
    }

    @Override
    public Component getDisplayName() {
        return this.getBlockState().getBlock().getName();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int pContainerId, Inventory pPlayerInventory, Player pPlayer) {
        return new DeskMenu(pContainerId, pPlayerInventory, this);
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        this.saveAdditional(tag);
        return tag;
    }

    @Override
    protected void saveAdditional(CompoundTag pTag) {
        super.saveAdditional(pTag);
        pTag.put("Items", this.ioHandler.serializeNBT());
        pTag.putBoolean("Minecraft", this.minecraft);
        if (!this.completedOutput.isEmpty()) {
            pTag.put("CompletedOutput", this.completedOutput.serializeNBT());
        }
        ListTag list = new ListTag();
        this.lock.forEach((item, set) -> {
            CompoundTag tag = new CompoundTag();
            tag.putString("Item", ForgeRegistries.ITEMS.getKey(item).toString());
            tag.putIntArray("Indexes", set.toIntArray());
            list.add(tag);
        });
        pTag.put("Lock", list);
    }

    @Override
    public void load(CompoundTag pTag) {
        super.load(pTag);
        this.ioHandler.deserializeNBT(pTag.getCompound("Items"));
        this.completedOutput = ItemStack.of(pTag.getCompound("CompletedOutput"));
        this.minecraft = pTag.getBoolean("Minecraft");

        Object2ObjectOpenHashMap<Item, IntAVLTreeSet> lock = new Object2ObjectOpenHashMap<>();
        for (Tag entry : pTag.getList("Lock", Tag.TAG_COMPOUND)) {
            CompoundTag tag = (CompoundTag) entry;
            ResourceLocation id = ResourceLocation.tryParse(tag.getString("Item"));
            if (id != null && ForgeRegistries.ITEMS.containsKey(id)) {
                lock.put(ForgeRegistries.ITEMS.getValue(id), new IntAVLTreeSet(tag.getIntArray("Indexes")));
            }
        }
        this.readLock(lock);
        this.updateRecipe();
    }
}
