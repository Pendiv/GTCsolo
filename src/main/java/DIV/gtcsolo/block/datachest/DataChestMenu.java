package DIV.gtcsolo.block.datachest;

import DIV.gtcsolo.registry.ModBlockEntities;
import DIV.gtcsolo.registry.ModMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.SlotItemHandler;

/**
 * DataChest UI container — 9×9 = 81 slot + プレイヤーインベ。
 *
 * <p>各 slot は {@link DataChestSlot} (= 自前) で、 maxStackSize 表示を override して
 * 大量格納を許容する。 quickMoveStack も override して shift+クリックで適切な
 * 量を一度に移動する。
 */
public class DataChestMenu extends AbstractContainerMenu {

    public static final int CHEST_COLS = 9;
    public static final int CHEST_ROWS = 9;
    public static final int CHEST_SLOTS = CHEST_COLS * CHEST_ROWS; // 81

    private static final int SLOT_PX = 18;
    private static final int CHEST_X = 8;
    private static final int CHEST_Y = 18;
    private static final int PLAYER_INV_X = 8;
    private static final int PLAYER_INV_Y = 18 + CHEST_ROWS * SLOT_PX + 14;

    private final DataChestBlockEntity be;
    private final BlockPos bePos;

    /** client constructor (network) */
    public DataChestMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, resolveBlockEntity(inv, buf.readBlockPos()));
    }

    /** server / shared constructor */
    public DataChestMenu(int id, Inventory inv, DataChestBlockEntity be) {
        super(ModMenuTypes.DATACHEST.get(), id);
        this.be = be;
        this.bePos = be != null ? be.getBlockPos() : null;
        // client でチャンク未同期等により BE が引けない場合はダミーで開く (stillValid=false で即閉じる)
        DataChestItemHandler items = be != null ? be.getItems() : new DataChestItemHandler(null);

        // 9×9 chest slots
        for (int row = 0; row < CHEST_ROWS; row++) {
            for (int col = 0; col < CHEST_COLS; col++) {
                int idx = row * CHEST_COLS + col;
                int x = CHEST_X + col * SLOT_PX;
                int y = CHEST_Y + row * SLOT_PX;
                addSlot(new DataChestSlot(items, idx, x, y));
            }
        }

        // Player inventory 3×9
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inv, col + row * 9 + 9,
                        PLAYER_INV_X + col * SLOT_PX,
                        PLAYER_INV_Y + row * SLOT_PX));
            }
        }
        // Player hotbar
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inv, col,
                    PLAYER_INV_X + col * SLOT_PX,
                    PLAYER_INV_Y + 3 * SLOT_PX + 4));
        }

        if (be != null) be.startOpen();
    }

    /** client 側で BE を再取得するための pos (= count overlay 表示で BE 経由で正確な count を読む) */
    public BlockPos getBePos() {
        return bePos;
    }

    private static DataChestBlockEntity resolveBlockEntity(Inventory inv, BlockPos pos) {
        BlockEntity be = inv.player.level().getBlockEntity(pos);
        if (be instanceof DataChestBlockEntity dc) return dc;
        return null;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (be != null) be.stopOpen();
    }

    @Override
    public boolean stillValid(Player player) {
        if (be == null || be.isRemoved()) return false;
        return player.distanceToSqr(be.getBlockPos().getX() + 0.5,
                be.getBlockPos().getY() + 0.5,
                be.getBlockPos().getZ() + 0.5) <= 64.0;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        Slot src = slots.get(slotIndex);
        if (!src.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = src.getItem();

        // chest ↔ player inv の移動
        if (slotIndex < CHEST_SLOTS) {
            // chest slot → player inv (= player slot range = CHEST_SLOTS 〜 CHEST_SLOTS+35)
            // chest 側は大量格納可能なので、 vanilla の moveItemStackTo は count=63 単位で
            // 動くため、 1 shift+click あたり「player inv 1 スタック分」 移動する
            int moveCount = Math.min(stack.getCount(), stack.getMaxStackSize());
            ItemStack toMove = stack.copy();
            toMove.setCount(moveCount);
            ItemStack remainder = toMove.copy();
            if (!moveItemStackTo(remainder, CHEST_SLOTS, CHEST_SLOTS + 36, true)) {
                return ItemStack.EMPTY;
            }
            int actuallyMoved = moveCount - remainder.getCount();
            stack.shrink(actuallyMoved);
            if (stack.isEmpty()) src.set(ItemStack.EMPTY);
            else src.setChanged();
        } else {
            // player inv → chest: vanilla の moveItemStackTo は
            // Math.min(slot.maxStack, stack.maxStack=64) で cap してしまうため使えない
            // (= 既存 64 越え slot に merge できず空 slot に新規流入する)。 自前 merge で
            // chest slot の真容量 (= maxStackSize × 335542) を上限として既存 slot 優先で詰める。
            int moved = mergeIntoChest(stack);
            if (moved == 0) return ItemStack.EMPTY;
            if (stack.isEmpty()) src.set(ItemStack.EMPTY);
            else src.setChanged();
        }
        return ItemStack.EMPTY;
    }

    /**
     * stack を chest slot 群に流し込む自前 merge。
     * <ol>
     *   <li>Pass 1: 既存同一アイテムスロットへ詰める (= 容量上限まで)</li>
     *   <li>Pass 2: 残ったぶんを空 slot に新規配置</li>
     * </ol>
     * stack 自身が shrink され、 移動した総量を返す (= 0 なら 1 つも入らなかった)。
     */
    private int mergeIntoChest(ItemStack stack) {
        int initial = stack.getCount();
        if (initial == 0) return 0;

        // Pass 1: 既存同一アイテムスロットへ merge
        for (int i = 0; i < CHEST_SLOTS && !stack.isEmpty(); i++) {
            Slot slot = slots.get(i);
            ItemStack existing = slot.getItem();
            if (existing.isEmpty()) continue;
            if (!ItemStack.isSameItemSameTags(existing, stack)) continue;
            int slotMax = slot.getMaxStackSize(stack); // DataChestSlot は maxStack × 335542
            int spaceLeft = slotMax - existing.getCount();
            if (spaceLeft <= 0) continue;
            int moveAmount = Math.min(spaceLeft, stack.getCount());
            existing.grow(moveAmount);
            stack.shrink(moveAmount);
            slot.setChanged(); // → DataChestSlot.setChanged → BE sync
        }

        // Pass 2: 空 slot に新規配置
        for (int i = 0; i < CHEST_SLOTS && !stack.isEmpty(); i++) {
            Slot slot = slots.get(i);
            if (!slot.getItem().isEmpty()) continue;
            int slotMax = slot.getMaxStackSize(stack);
            int moveAmount = Math.min(slotMax, stack.getCount());
            ItemStack toPlace = stack.copy();
            toPlace.setCount(moveAmount);
            slot.set(toPlace); // → SlotItemHandler.set → setStackInSlot → onContentsChanged → BE sync
            stack.shrink(moveAmount);
        }

        return initial - stack.getCount();
    }
}
