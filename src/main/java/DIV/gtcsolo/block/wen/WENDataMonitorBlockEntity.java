package DIV.gtcsolo.block.wen;

import DIV.gtcsolo.machine.WENMainStorageMachine;
import DIV.gtcsolo.network.wen.WENMonitorUpdatePacket;
import DIV.gtcsolo.network.ModNetwork;
import DIV.gtcsolo.registry.ModBlockEntities;
import com.gregtechceu.gtceu.api.blockentity.MetaMachineBlockEntity;
import com.mojang.logging.LogUtils;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

/**
 * WEN Data Monitor BlockEntity
 * マルチブロック形成時にコントローラー位置が設定される。
 * UIからネットワークIDの設定や蓄電状況の確認ができる。
 */
public class WENDataMonitorBlockEntity extends BlockEntity implements MenuProvider {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** リンク先コントローラーの位置 (未リンク時はnull) */
    @Nullable
    private BlockPos controllerPos = null;

    private int syncTimer = 0;

    public WENDataMonitorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WEN_DATA_MONITOR.get(), pos, state);
    }

    // =========================================================================
    //  コントローラーリンク
    // =========================================================================

    public void setControllerPos(@Nullable BlockPos pos) {
        this.controllerPos = pos;
        setChanged();
        LOGGER.info("[WEN Monitor] Controller linked at {}", pos);
    }

    @Nullable
    public BlockPos getControllerPos() { return controllerPos; }

    @Nullable
    public WENMainStorageMachine getController() {
        if (controllerPos == null || level == null) return null;
        BlockEntity be = level.getBlockEntity(controllerPos);
        if (be instanceof MetaMachineBlockEntity mmbe) {
            if (mmbe.getMetaMachine() instanceof WENMainStorageMachine wen) {
                return wen;
            }
        }
        return null;
    }

    // =========================================================================
    //  MenuProvider
    // =========================================================================

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.gtcsolo.wen_data_monitor");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        WENMainStorageMachine wen = getController();
        String netId = wen != null ? wen.getNetworkId() : "";
        long stored = wen != null ? wen.getStoredEnergy() : 0;
        long capacity = wen != null ? wen.getMaxCapacityLong() : 0;
        boolean formed = wen != null && wen.isFormed();
        return new WENDataMonitorMenu(id, worldPosition, netId, stored, capacity, formed);
    }

    public void writeScreenOpenData(FriendlyByteBuf buf) {
        buf.writeBlockPos(worldPosition);
        WENMainStorageMachine wen = getController();
        buf.writeUtf(wen != null ? wen.getNetworkId() : "");
        buf.writeLong(wen != null ? wen.getStoredEnergy() : 0);
        buf.writeLong(wen != null ? wen.getMaxCapacityLong() : 0);
        buf.writeBoolean(wen != null && wen.isFormed());
    }

    // =========================================================================
    //  定期UI同期
    // =========================================================================

    /** 即座に全viewerにデータを同期する */
    public void forceSyncNow() {
        if (level != null && !level.isClientSide) {
            syncToViewers(level, worldPosition);
        }
    }

    public static void tick(Level level, BlockPos pos, BlockState state, WENDataMonitorBlockEntity be) {
        if (level.isClientSide) return;
        be.syncTimer++;
        if (be.syncTimer < 20) return;
        be.syncTimer = 0;
        be.syncToViewers(level, pos);
    }

    private void syncToViewers(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        WENMainStorageMachine wen = getController();

        DIV.gtcsolo.machine.wen.WENNetworkData data =
                DIV.gtcsolo.machine.wen.WENNetworkData.get(serverLevel.getServer().overworld());

        String netId = wen != null ? wen.getNetworkId() : "";
        DIV.gtcsolo.machine.wen.WENNetworkData.WENEntry entry =
                !netId.isEmpty() ? data.getNetwork(netId) : null;

        long stored = entry != null ? entry.storedEnergy.min(java.math.BigInteger.valueOf(Long.MAX_VALUE)).longValue() : 0;
        long cap = entry != null ? entry.maxCapacity.min(java.math.BigInteger.valueOf(Long.MAX_VALUE)).longValue() : 0;
        String storedStr = entry != null ? entry.storedEnergy.toString() : "0";
        String capacityStr = entry != null ? entry.maxCapacity.toString() : "0";
        boolean formed = wen != null && wen.isFormed();
        long inps = entry != null ? entry.inputThisTick : 0;
        long outps = entry != null ? entry.outputThisTick : 0;
        // BigInteger履歴をlong（クランプ）に変換
        java.util.List<Long> history = new java.util.ArrayList<>();
        if (entry != null) {
            for (java.math.BigInteger bi : entry.energyHistory) {
                history.add(bi.min(java.math.BigInteger.valueOf(Long.MAX_VALUE)).longValue());
            }
        }

        // 全ネットワーク一覧
        java.util.List<WENDataMonitorMenu.NetworkInfo> allNets = new java.util.ArrayList<>();
        for (String id : data.getAllNetworkIds()) {
            var e = data.getNetwork(id);
            if (e != null) {
                allNets.add(new WENDataMonitorMenu.NetworkInfo(
                        id, e.isFormed(),
                        e.storedEnergy.min(java.math.BigInteger.valueOf(Long.MAX_VALUE)).longValue(),
                        e.maxCapacity.min(java.math.BigInteger.valueOf(Long.MAX_VALUE)).longValue(),
                        e.dimension));
            }
        }
        allNets.sort(java.util.Comparator.comparing(WENDataMonitorMenu.NetworkInfo::id));

        // 履歴記録 (1秒ごと)
        data.recordHistory();

        for (ServerPlayer player : serverLevel.getServer().getPlayerList().getPlayers()) {
            if (player.containerMenu instanceof WENDataMonitorMenu menu
                    && menu.getMonitorPos().equals(pos)) {
                boolean isOp = player.hasPermissions(2);
                boolean crossDim = entry != null && entry.crossDimensionEnabled;
                String[] upgradeNames = new String[5];
                java.util.Arrays.fill(upgradeNames, "");
                if (wen != null && wen.hasCrossDimCrystal()) {
                    upgradeNames[0] = "Evernight Crystal";
                }
                int storageLv = wen != null ? wen.getStorageUpgradeLevel() : 0;
                int nextCost = wen != null ? wen.getNextUpgradeCost() : 2;
                WENMonitorUpdatePacket packet = new WENMonitorUpdatePacket(
                        pos, netId, stored, cap, formed, inps, outps, history, allNets, isOp,
                        crossDim, upgradeNames, storedStr, capacityStr,
                        storageLv, nextCost);
                ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
            }
        }
    }

    // =========================================================================
    //  NBT
    // =========================================================================

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("CtrlX")) {
            controllerPos = new BlockPos(tag.getInt("CtrlX"), tag.getInt("CtrlY"), tag.getInt("CtrlZ"));
        }
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