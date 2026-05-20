package DIV.gtcsolo.block.datachest;

import DIV.gtcsolo.registry.ModBlockEntities;
import DIV.gtcsolo.registry.ModMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

/**
 * DataChest BlockEntity — 81 スロットのインベントリ + open count tracking + sync。
 *
 * <p>open count = 「現在この chest を開いてる player の数」 (vanilla ChestBlockEntity と同パターン)。
 * 増減時に BlockState の OPEN プロパティを更新 → texture が切り替わる。
 */
public class DataChestBlockEntity extends BlockEntity implements MenuProvider {

    private final DataChestItemHandler items = new DataChestItemHandler(this);
    private final LazyOptional<IItemHandler> itemsCap = LazyOptional.of(() -> items);

    private int openCount = 0;

    public DataChestBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DATACHEST.get(), pos, state);
    }

    public DataChestItemHandler getItems() {
        return items;
    }

    /** tick: 何もしないが BlockEntityTicker 経由で呼ばれる (= 将来的な拡張用 hook) */
    public void tick() {}

    // ─── open / close tracking ───

    public void startOpen() {
        if (this.remove) return;
        if (openCount == 0) updateOpenState(true);
        openCount++;
    }

    public void stopOpen() {
        if (this.remove) return;
        openCount = Math.max(0, openCount - 1);
        if (openCount == 0) updateOpenState(false);
    }

    private void updateOpenState(boolean open) {
        if (level == null) return;
        BlockState state = getBlockState();
        if (state.getBlock() instanceof DataChestBlock) {
            BlockState newState = state.setValue(DataChestBlock.OPEN, open);
            level.setBlock(worldPosition, newState, 3);
        }
    }

    // ─── sync ───

    public void syncToClient() {
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        tag.put("Items", items.serializeNBT());
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        super.handleUpdateTag(tag);
        if (tag.contains("Items")) items.deserializeNBT(tag.getCompound("Items"));
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        super.onDataPacket(net, pkt);
        CompoundTag tag = pkt.getTag();
        if (tag != null && tag.contains("Items")) items.deserializeNBT(tag.getCompound("Items"));
    }

    // ─── NBT save / load ───

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Items", items.serializeNBT());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("Items")) items.deserializeNBT(tag.getCompound("Items"));
    }

    // ─── capability ───

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) return itemsCap.cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        itemsCap.invalidate();
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        itemsCap.invalidate();
    }

    // ─── MenuProvider ───

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.gtcsolo.datachest");
    }

    /** NetworkHooks.openScreen の 3rd 引数 = client menu factory に渡す BlockPos */
    public void writeScreenOpenData(FriendlyByteBuf buf) {
        buf.writeBlockPos(worldPosition);
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inv, Player player) {
        return new DataChestMenu(containerId, inv, this);
    }
}
