package DIV.gtcsolo.machine.cloud;

import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.UITemplate;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.lowdragmc.lowdraglib.gui.modular.IUIHolder;
import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib.gui.widget.ButtonWidget;
import com.lowdragmc.lowdraglib.gui.widget.ComponentPanelWidget;
import com.lowdragmc.lowdraglib.gui.widget.SlotWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class CloudStorageUi {
    private static final int PAGE_SIZE = 45;

    private CloudStorageUi() {}

    public static ModularUI createChestUI(CloudStorageMachine machine, Player player) {
        var page = new ChestPage(machine, player);
        var root = new WidgetGroup(0, 0, 176, 221);
        root.setBackground(GuiTextures.BACKGROUND);
        root.addWidget(new ComponentPanelWidget(8, 7, List.of(machine.getBlockState().getBlock().getName())));
        root.addWidget(new ComponentPanelWidget(8, 21, lines -> {
            if (!page.isRemote() && CloudStorageData.validColor(page.color)) lines.add(channel(page.color));
        }));
        root.addWidget(new PageSync(page));
        for (int slot = 0; slot < PAGE_SIZE; slot++) {
            root.addWidget(new ChestSlot(page, slot, 7 + (slot % 9) * 18, 36 + (slot / 9) * 18));
        }
        root.addWidget(new PageButton(page, 7, -1));
        root.addWidget(new ComponentPanelWidget(37, 131, lines -> {
            if (!page.isRemote()) {
                lines.add(Component.translatable("gtcsolo.cloud.ui.page",
                        page.page + 1, Math.max(page.page + 1, page.pageCount())));
            }
        }));
        root.addWidget(new PageButton(page, 151, 1));
        root.addWidget(UITemplate.bindPlayerInventory(player.getInventory(), GuiTextures.SLOT, 7, 146, true));
        return new ModularUI(root, page, player);
    }

    static Component channel(int color) {
        return Component.translatable("gtcsolo.cloud.ui.channel", dyeName(DyeColor.byId(color)));
    }

    static Component dyeName(DyeColor dye) {
        return Component.translatable("item.minecraft." + dye.getName() + "_dye");
    }

    static boolean isNear(Player player, MetaMachine machine) {
        var pos = machine.getPos();
        return player.level() == machine.getLevel() && !machine.isInValid()
                && player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64;
    }

    private static final class ChestPage implements Container, IUIHolder {
        private final CloudStorageMachine machine;
        private final Player player;
        private final int color;
        private final @Nullable SimpleContainer clientInventory;
        private int page;
        private int turnCount;
        private boolean waitingForPage;
        private int syncedVisible;
        private long syncedWritable;

        private ChestPage(CloudStorageMachine machine, Player player) {
            this.machine = machine;
            this.player = player;
            this.color = machine.getColor();
            this.clientInventory = player.level().isClientSide ? new SimpleContainer(PAGE_SIZE) : null;
        }

        private @Nullable CloudStorageData data() {
            return !isRemote() && !isInvalid() ? machine.getCloudData() : null;
        }

        @Override
        public boolean isInvalid() {
            return !isRemote() && (!isNear(player, machine) || !CloudStorageData.validColor(color)
                    || machine.getColor() != color || !machine.isOperational());
        }

        @Override
        public boolean isRemote() {
            return clientInventory != null;
        }

        @Override
        public ModularUI createUI(Player player) {
            return createChestUI(machine, player);
        }

        @Override
        public void markAsDirty() {}

        private void turn(int direction) {
            turnCount++;
            if (!isInvalid()) page = Math.max(0, Math.min(pageCount() - 1, page + direction));
        }

        private int pageCount() {
            var data = data();
            return data == null ? 1 : Math.max(1, (data.itemSlots(color) + PAGE_SIZE - 1) / PAGE_SIZE);
        }

        private int slotIndex(int slot) {
            return page * PAGE_SIZE + slot;
        }

        private int visible() {
            if (isRemote()) return syncedVisible;
            var data = data();
            return data == null ? 0 : Math.max(0, Math.min(PAGE_SIZE, data.itemSlots(color) - slotIndex(0)));
        }

        private boolean isWritable(int slot) {
            if (isRemote()) return (syncedWritable & (1L << slot)) != 0;
            var data = data();
            return data != null && data.isItemSlotWritable(color, slotIndex(slot));
        }

        private long writableMask() {
            var data = data();
            if (data == null) return 0;
            long mask = 0;
            for (int slot = 0; slot < PAGE_SIZE; slot++) {
                if (data.isItemSlotWritable(color, slotIndex(slot))) mask |= 1L << slot;
            }
            return mask;
        }

        @Override
        public int getContainerSize() {
            return PAGE_SIZE;
        }

        @Override
        public boolean isEmpty() {
            for (int slot = 0; slot < PAGE_SIZE; slot++) {
                if (!getItem(slot).isEmpty()) return false;
            }
            return true;
        }

        @Override
        public ItemStack getItem(int slot) {
            if (clientInventory != null) return clientInventory.getItem(slot);
            var data = data();
            // Native slot merging mutates the stored stack before calling setChanged.
            return data == null ? ItemStack.EMPTY : data.itemReferenceForMenu(color, slotIndex(slot));
        }

        @Override
        public ItemStack removeItem(int slot, int count) {
            if (clientInventory != null) return clientInventory.removeItem(slot, count);
            var data = data();
            return data == null ? ItemStack.EMPTY : data.extractItem(color, slotIndex(slot), count, false);
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
            return removeItem(slot, Integer.MAX_VALUE);
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
            if (clientInventory != null) {
                clientInventory.setItem(slot, stack);
            } else {
                var data = data();
                if (data != null) data.setItemInSlot(color, slotIndex(slot), stack);
            }
        }

        private void slotChanged(int slot) {
            var data = data();
            if (data != null) data.itemChangedByMenu(color, slotIndex(slot));
        }

        @Override
        public void setChanged() {}

        @Override
        public boolean stillValid(Player player) {
            return this.player == player && !isInvalid();
        }

        @Override
        public void clearContent() {
            if (clientInventory != null) clientInventory.clearContent();
        }
    }

    private static final class PageButton extends ButtonWidget {
        private final ChestPage page;

        private PageButton(ChestPage page, int x, int direction) {
            super(x, 129, 18, 14, direction < 0 ? GuiTextures.BUTTON_LEFT : GuiTextures.BUTTON_RIGHT, click -> {
                if (click.isRemote) page.waitingForPage = true;
                else page.turn(direction);
            });
            this.page = page;
        }

        @Override
        public boolean isActive() {
            return !page.waitingForPage && super.isActive();
        }
    }

    private static final class ChestSlot extends SlotWidget {
        private final ChestPage page;

        private ChestSlot(ChestPage page, int slot, int x, int y) {
            super(page, slot, x, y, true, true);
            this.page = page;
            setBackgroundTexture(GuiTextures.SLOT);
        }

        @Override
        public boolean isEnabled() {
            return !page.waitingForPage && super.isEnabled();
        }

        @Override
        protected Slot createSlot(Container container, int slot) {
            var page = (ChestPage) container;
            return new WidgetSlot(container, slot, 0, 0) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return page.isWritable(slot) && super.mayPlace(stack);
                }

                @Override
                public boolean mayPickup(Player player) {
                    return page.stillValid(player) && slot < page.visible() && super.mayPickup(player);
                }

                @Override
                public void setChanged() {
                    page.slotChanged(slot);
                    super.setChanged();
                }
            };
        }
    }

    private static final class PageSync extends Widget {
        private final ChestPage page;
        private int syncedTurnCount;

        private PageSync(ChestPage page) {
            super(0, 0, 0, 0);
            this.page = page;
        }

        @Override
        public void writeInitialData(FriendlyByteBuf buffer) {
            page.syncedVisible = page.visible();
            page.syncedWritable = page.writableMask();
            writeSlots(buffer);
        }

        @Override
        public void readInitialData(FriendlyByteBuf buffer) {
            readSlots(buffer);
        }

        @Override
        public void detectAndSendChanges() {
            int visible = page.visible();
            long writable = page.writableMask();
            if (visible == page.syncedVisible && writable == page.syncedWritable
                    && page.turnCount == syncedTurnCount) return;
            page.syncedVisible = visible;
            page.syncedWritable = writable;
            syncedTurnCount = page.turnCount;
            writeUpdateInfo(1, this::writeSlots);
        }

        @Override
        public void readUpdateInfo(int id, FriendlyByteBuf buffer) {
            if (id == 1) readSlots(buffer);
        }

        private void writeSlots(FriendlyByteBuf buffer) {
            buffer.writeVarInt(page.syncedVisible);
            buffer.writeLong(page.syncedWritable);
            buffer.writeVarInt(syncedTurnCount);
        }

        private void readSlots(FriendlyByteBuf buffer) {
            page.syncedVisible = buffer.readVarInt();
            page.syncedWritable = buffer.readLong();
            int acknowledgedTurns = buffer.readVarInt();
            if (acknowledgedTurns != syncedTurnCount) page.waitingForPage = false;
            syncedTurnCount = acknowledgedTurns;
        }
    }
}
