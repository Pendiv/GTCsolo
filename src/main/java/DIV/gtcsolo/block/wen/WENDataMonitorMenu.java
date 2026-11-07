package DIV.gtcsolo.block.wen;

import DIV.gtcsolo.machine.wen.WENMonitorData;
import DIV.gtcsolo.network.wen.WENMonitorUpdatePacket;
import DIV.gtcsolo.registry.ModMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.math.BigInteger;
import java.util.List;

public class WENDataMonitorMenu extends AbstractContainerMenu {
    private final BlockPos monitorPos;
    private WENMonitorData data;
    private int viewPage, networkOffset;
    private double fillFraction;
    private BigInteger historyMaximum = BigInteger.ZERO;
    private double[] historyFractions = new double[0], networkFractions = new double[0];

    public WENDataMonitorMenu(int id, BlockPos pos, WENMonitorData data) {
        super(ModMenuTypes.WEN_DATA_MONITOR.get(), id);
        monitorPos = pos.immutable();
        updateData(data);
    }

    public WENDataMonitorMenu(int id, Inventory inventory, FriendlyByteBuf buffer) {
        this(id, buffer.readBlockPos(), WENMonitorUpdatePacket.readData(buffer));
    }

    public void updateData(WENMonitorData value) {
        data = value;
        fillFraction = WENMonitorData.fraction(data.stored(), data.capacity());
        historyMaximum = data.history().stream().max(BigInteger::compareTo).orElse(BigInteger.ZERO);
        historyFractions =
                data.history().stream()
                        .mapToDouble(v -> WENMonitorData.fraction(v, historyMaximum))
                        .toArray();
        networkFractions =
                data.networks().stream()
                        .mapToDouble(n -> WENMonitorData.fraction(n.stored(), n.capacity()))
                        .toArray();
    }

    public void setView(int page, int offset) {
        viewPage = Math.max(0, Math.min(WENMonitorData.PAGE_COUNT - 1, page));
        networkOffset = Math.max(0, offset);
    }

    public WENMonitorData data() {
        return data;
    }

    public BlockPos getMonitorPos() {
        return monitorPos;
    }

    public int getViewPage() {
        return viewPage;
    }

    public int getNetworkOffset() {
        return networkOffset;
    }

    public double getFillFraction() {
        return fillFraction;
    }

    public BigInteger getHistoryMaximum() {
        return historyMaximum;
    }

    public double getHistoryFraction(int index) {
        return historyFractions[index];
    }

    public double getNetworkFraction(int index) {
        return networkFractions[index];
    }

    public String getNetworkId() {
        return data.networkId();
    }

    public long getStored() {
        return data.stored().min(BigInteger.valueOf(Long.MAX_VALUE)).longValue();
    }

    public long getCapacity() {
        return data.capacity().min(BigInteger.valueOf(Long.MAX_VALUE)).longValue();
    }

    public String getStoredStr() {
        return data.stored().toString();
    }

    public String getCapacityStr() {
        return data.capacity().toString();
    }

    public boolean isFormed() {
        return data.formed();
    }

    public boolean isOp() {
        return data.operator();
    }

    public boolean isCrossDimensionEnabled() {
        return data.crossDimension();
    }

    public int getStorageLevel() {
        return data.storageLevel();
    }

    public int getNextUpgradeCost() {
        return data.nextUpgradeCost();
    }

    public long getInputPerSec() {
        return data.inputPerSecond();
    }

    public long getOutputPerSec() {
        return data.outputPerSecond();
    }

    public List<BigInteger> getEnergyHistory() {
        return data.history();
    }

    public List<WENMonitorData.NetworkInfo> getAllNetworks() {
        return data.networks();
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return player.isAlive()
                && player.level().hasChunkAt(monitorPos)
                && player.distanceToSqr(
                                monitorPos.getX() + 0.5,
                                monitorPos.getY() + 0.5,
                                monitorPos.getZ() + 0.5)
                        <= 64.0
                && player.level().getBlockEntity(monitorPos) instanceof WENDataMonitorBlockEntity;
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int slot) {
        return ItemStack.EMPTY;
    }
}
