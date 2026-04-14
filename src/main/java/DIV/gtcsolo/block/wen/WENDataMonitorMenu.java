package DIV.gtcsolo.block.wen;

import DIV.gtcsolo.registry.ModMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class WENDataMonitorMenu extends AbstractContainerMenu {

    private final BlockPos monitorPos;
    private String networkId;
    private long stored; // longで受け渡し（表示用、精度は十分）
    private long capacity; // longで受け渡し（2^63-1超えの場合はLong.MAX_VALUE）
    private String storedStr = "0"; // BigInteger文字列表現
    private String capacityStr = "0";
    private boolean formed;
    private long inputPerSec;
    private long outputPerSec;
    private final List<Long> energyHistory = new ArrayList<>();
    private final List<NetworkInfo> allNetworks = new ArrayList<>();
    private boolean isOp;
    private boolean crossDimensionEnabled;
    private final String[] upgradeSlotNames = {"", "", "", "", ""};
    private int storageLevel = 0;
    private int nextUpgradeCost = 2;

    public record NetworkInfo(String id, boolean formed, long stored, long capacity, String dimension) {}

    /** サーバーサイド */
    public WENDataMonitorMenu(int id, BlockPos pos, String netId,
                               long stored, long capacity, boolean formed) {
        super(ModMenuTypes.WEN_DATA_MONITOR.get(), id);
        this.monitorPos = pos;
        this.networkId = netId;
        this.stored = stored;
        this.capacity = capacity;
        this.formed = formed;
    }

    /** クライアントサイド */
    public WENDataMonitorMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        super(ModMenuTypes.WEN_DATA_MONITOR.get(), id);
        this.monitorPos = buf.readBlockPos();
        this.networkId = buf.readUtf(64);
        this.stored = buf.readLong();
        this.capacity = buf.readLong();
        this.formed = buf.readBoolean();
    }

    // --- Getters ---
    public BlockPos getMonitorPos() { return monitorPos; }
    public String getNetworkId() { return networkId; }
    public long getStored() { return stored; }
    public long getCapacity() { return capacity; }
    public String getStoredStr() { return storedStr; }
    public String getCapacityStr() { return capacityStr; }
    public boolean isFormed() { return formed; }
    public long getInputPerSec() { return inputPerSec; }
    public long getOutputPerSec() { return outputPerSec; }
    public List<Long> getEnergyHistory() { return energyHistory; }
    public List<NetworkInfo> getAllNetworks() { return allNetworks; }
    public boolean isOp() { return isOp; }
    public boolean isCrossDimensionEnabled() { return crossDimensionEnabled; }
    public String[] getUpgradeSlotNames() { return upgradeSlotNames; }
    public int getStorageLevel() { return storageLevel; }
    public int getNextUpgradeCost() { return nextUpgradeCost; }

    public void updateData(String networkId, long stored, long capacity, boolean formed,
                           long inputPerSec, long outputPerSec,
                           List<Long> history, List<NetworkInfo> networks, boolean isOp,
                           boolean crossDim, String[] upgradeNames,
                           String storedStr, String capacityStr,
                           int storageLv, int nextCost) {
        this.networkId = networkId;
        this.stored = stored;
        this.capacity = capacity;
        this.storedStr = storedStr;
        this.capacityStr = capacityStr;
        this.formed = formed;
        this.inputPerSec = inputPerSec;
        this.outputPerSec = outputPerSec;
        this.energyHistory.clear();
        this.energyHistory.addAll(history);
        this.allNetworks.clear();
        this.allNetworks.addAll(networks);
        this.isOp = isOp;
        this.crossDimensionEnabled = crossDim;
        if (upgradeNames != null) {
            for (int i = 0; i < Math.min(upgradeNames.length, 5); i++) {
                this.upgradeSlotNames[i] = upgradeNames[i];
            }
        }
        this.storageLevel = storageLv;
        this.nextUpgradeCost = nextCost;
    }

    @Override public boolean stillValid(@NotNull Player player) { return true; }
    @Override public @NotNull ItemStack quickMoveStack(@NotNull Player p, int i) { return ItemStack.EMPTY; }
}