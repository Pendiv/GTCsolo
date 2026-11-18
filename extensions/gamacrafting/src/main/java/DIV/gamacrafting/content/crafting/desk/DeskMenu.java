package DIV.gamacrafting.content.crafting.desk;

import DIV.gamacrafting.Gamacrafting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;

public class DeskMenu extends AbstractContainerMenu {
    private static final int PLAYER_INVENTORY_START = 1 + DeskBlockEntity.GRID_SIZE;
    private static final int HOTBAR_START = PLAYER_INVENTORY_START + 27;
    public final ContainerLevelAccess access;
    private final DeskBlockEntity desk;
    private final ContainerData data;
    public boolean canCraft = false;

    public DeskMenu(int id, Inventory inventory, FriendlyByteBuf buf) {
        this(id, inventory, inventory.player.level().getBlockEntity(buf.readBlockPos(), Gamacrafting.DESK_BLOCK_ENTITY.get()).orElseThrow());
    }

    public DeskMenu(int id, Inventory inventory, DeskBlockEntity desk) {
        super(Gamacrafting.DESK_MENU.get(), id);
        this.desk = desk;
        this.access = ContainerLevelAccess.create(inventory.player.level(), desk.getBlockPos());
        this.data = desk.data;
        this.addSlot(new SlotItemHandler(desk.result, 0, 80, 188) {

            @Override
            public boolean mayPickup(Player playerIn) {
                if (DeskMenu.this.isMinecraft()) {
                    return false;
                } else {
                    if (!DeskMenu.this.canCraft && this.hasItem()) {
                        playerIn.playNotifySound(Gamacrafting.DESK_RESULT.get(), SoundSource.MASTER, 0.25f, 1);
                    }
                    return true;
                }
            }

            @Override
            public int getMaxStackSize(@NotNull ItemStack stack) {
                return Math.min(this.getMaxStackSize(), stack.getMaxStackSize());
            }
        });

        for (int row = 0; row < DeskBlockEntity.GRID_WIDTH; ++row) {
            for (int column = 0; column < DeskBlockEntity.GRID_WIDTH; ++column) {
                this.addSlot(new SlotItemHandler(desk.inventory, column + row * DeskBlockEntity.GRID_WIDTH, 8 + column * 18, 18 + row * 18) {
                    @Override
                    public boolean mayPickup(Player playerIn) {
                        return true;
                    }

                    @Override
                    public int getMaxStackSize(@NotNull ItemStack stack) {
                        return Math.min(this.getMaxStackSize(), stack.getMaxStackSize());
                    }
                });
            }
        }

        for (int column = 0; column < 3; ++column) {
            for (int row = 0; row < 9; ++row) {
                this.addSlot(new Slot(inventory, row + column * 9 + 9, 8 + row * 18, 214 + column * 18));
            }
        }
        for (int row = 0; row < 9; ++row) {
            this.addSlot(new Slot(inventory, row, 8 + row * 18, 272));
        }

        this.addDataSlots(this.data);
        desk.updateRecipe();
    }

    @Override
    public boolean stillValid(Player pPlayer) {
        return !this.desk.isRemoved() && this.desk.getLevel() == pPlayer.level()
                && stillValid(this.access, pPlayer, Gamacrafting.DESK_BLOCK.get());
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (slotId == 0 || clickType == ClickType.PICKUP_ALL) {
            this.desk.updateRecipe();
        }
        if (slotId != 0 || clickType != ClickType.SWAP) {
            super.clicked(slotId, button, clickType, player);
            return;
        }

        Inventory inventory = player.getInventory();
        Slot output = this.getSlot(0);
        if ((button >= 0 && button < 9 || button == Inventory.SLOT_OFFHAND)
                && inventory.getItem(button).isEmpty() && output.hasItem() && output.mayPickup(player)) {
            inventory.setItem(button, output.remove(inventory.getMaxStackSize()));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player pPlayer, int pIndex) {
        if (pIndex == 0) {
            return this.quickMoveResult();
        }
        Slot slot = this.getSlot(pIndex);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack source = slot.getItem();
        ItemStack ret = source.copy();

        if (pIndex < PLAYER_INVENTORY_START) {
            if (!this.moveItemStackTo(source, PLAYER_INVENTORY_START, this.slots.size(), false)) {
                return ItemStack.EMPTY;
            }
        } else if (!this.moveItemStackTo(source, 1, PLAYER_INVENTORY_START, false)) {
            if (pIndex < HOTBAR_START) {
                if (!this.moveItemStackTo(source, HOTBAR_START, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(source, PLAYER_INVENTORY_START, HOTBAR_START, false)) {
                return ItemStack.EMPTY;
            }
        }

        if (source.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        this.desk.inventoryChanged();
        return ret;
    }

    private ItemStack quickMoveResult() {
        ItemStack output = this.desk.result.extractItem(0, Integer.MAX_VALUE, true);
        if (output.isEmpty()) {
            return ItemStack.EMPTY;
        }

        ItemStack toMove = output.copy();
        if (!this.moveItemStackTo(toMove, PLAYER_INVENTORY_START, this.slots.size(), false)) {
            return ItemStack.EMPTY;
        }
        return this.getSlot(0).remove(output.getCount() - toMove.getCount());
    }

    public boolean isMinecraft() {
        return this.data.get(0) == 1;
    }
}
