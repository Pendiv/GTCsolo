package DIV.gtcsolo.block.wen;

import DIV.gtcsolo.machine.WENMainStorageMachine;
import DIV.gtcsolo.machine.wen.WENMonitorData;
import DIV.gtcsolo.machine.wen.WENNetworkData;
import DIV.gtcsolo.network.ModNetwork;
import DIV.gtcsolo.network.wen.WENMonitorUpdatePacket;
import DIV.gtcsolo.registry.ModBlockEntities;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.network.PacketDistributor;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** A passive monitor. History and synchronization are driven once per second by the server. */
public class WENDataMonitorBlockEntity extends BlockEntity implements MenuProvider {
    private BlockPos controllerPos;

    public WENDataMonitorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WEN_DATA_MONITOR.get(), pos, state);
    }

    public void setControllerPos(BlockPos pos) {
        if (!Objects.equals(controllerPos, pos)) {
            controllerPos = pos.immutable();
            setChanged();
        }
    }

    public WENMainStorageMachine getController() {
        if (level == null || controllerPos == null || !level.hasChunkAt(controllerPos)) return null;
        return MetaMachine.getMachine(level, controllerPos) instanceof WENMainStorageMachine core
                        && core.containsStructurePosition(worldPosition)
                ? core
                : null;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.gtcsolo.wen_data_monitor");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new WENDataMonitorMenu(id, worldPosition, snapshot(0, 0, player.hasPermissions(2)));
    }

    public void writeScreenOpenData(FriendlyByteBuf buffer, ServerPlayer player) {
        buffer.writeBlockPos(worldPosition);
        WENMonitorUpdatePacket.writeData(buffer, snapshot(0, 0, player.hasPermissions(2)));
    }

    public WENMonitorData snapshot(int page, int requestedOffset, boolean operator) {
        if (!(level instanceof ServerLevel serverLevel)) return WENMonitorData.EMPTY;
        WENMainStorageMachine core = getController();
        WENNetworkData data = WENNetworkData.find(serverLevel.getServer().overworld());
        String id = core == null ? "" : core.getNetworkId();
        var entry = data == null ? null : data.getNetwork(id);
        BigInteger stored = entry == null ? BigInteger.ZERO : entry.storedEnergy;
        BigInteger capacity =
                entry != null
                        ? entry.maxCapacity
                        : core == null ? BigInteger.ZERO : core.getMaxCapacity();
        List<BigInteger> history =
                page == 2 && entry != null ? List.copyOf(entry.energyHistory) : List.of();
        var networks = new ArrayList<WENMonitorData.NetworkInfo>();
        int offset = 0, count = 0;
        if (page == 3 && data != null) {
            List<String> ids = data.getSortedNetworkIds();
            count = ids.size();
            offset =
                    Math.max(
                            0,
                            Math.min(
                                    requestedOffset,
                                    Math.max(0, count - WENMonitorData.NETWORK_ROWS)));
            for (int i = offset; i < Math.min(count, offset + WENMonitorData.NETWORK_ROWS); i++) {
                String networkId = ids.get(i);
                var network = data.getNetwork(networkId);
                networks.add(
                        new WENMonitorData.NetworkInfo(
                                networkId,
                                network.isFormed(),
                                network.storedEnergy,
                                network.maxCapacity,
                                network.dimension));
            }
        }
        int storageLevel =
                core == null
                        ? 0
                        : Math.max(
                                0,
                                Math.min(
                                        WENMainStorageMachine.MAX_UPGRADE_LEVEL,
                                        core.getStorageUpgradeLevel()));
        return new WENMonitorData(
                id,
                stored,
                capacity,
                core != null && core.isNetworkReady(),
                entry == null ? 0 : entry.inputLastPeriod,
                entry == null ? 0 : entry.outputLastPeriod,
                history,
                networks,
                offset,
                count,
                operator,
                core != null && core.hasCrossDimCrystal(),
                storageLevel,
                2 * (storageLevel + 1),
                page);
    }

    public void syncTo(ServerPlayer player) {
        if (player.level() != level
                || !(player.containerMenu instanceof WENDataMonitorMenu menu)
                || !menu.getMonitorPos().equals(worldPosition)
                || !menu.stillValid(player)) return;
        var data = snapshot(menu.getViewPage(), menu.getNetworkOffset(), player.hasPermissions(2));
        ModNetwork.CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new WENMonitorUpdatePacket(menu.containerId, worldPosition, data));
    }

    public void forceSyncNow() {
        if (level instanceof ServerLevel serverLevel)
            for (ServerPlayer player : serverLevel.players()) syncTo(player);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        controllerPos =
                tag.contains("CtrlX")
                        ? new BlockPos(
                                tag.getInt("CtrlX"), tag.getInt("CtrlY"), tag.getInt("CtrlZ"))
                        : null;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (controllerPos != null) {
            tag.putInt("CtrlX", controllerPos.getX());
            tag.putInt("CtrlY", controllerPos.getY());
            tag.putInt("CtrlZ", controllerPos.getZ());
        }
    }
}
