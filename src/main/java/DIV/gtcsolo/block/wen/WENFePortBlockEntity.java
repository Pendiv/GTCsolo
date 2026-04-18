package DIV.gtcsolo.block.wen;

import DIV.gtcsolo.machine.wen.WENNetworkData;
import DIV.gtcsolo.registry.ModBlockEntities;
import DIV.gtcsolo.registry.ModBlocks;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.math.BigInteger;
import java.util.List;
import java.util.stream.Collectors;

/**
 * FE Wireless Port BE: Forge Energy ↔ WEN 双方向変換 (1 EU = 4 FE)。
 * Capability経由でFEケーブル/機械と直接やり取り + 出力ポートは10tick毎に隣接受信者に push。
 */
public class WENFePortBlockEntity extends BlockEntity {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static final int FE_PER_EU = 4;
    private static final int MAX_FE_PER_TICK_PUSH = 1_000_000_000;
    private static final int TICK_INTERVAL = 10;

    private final boolean isInput;
    private String linkedNetworkId = "";
    private int tickCounter = 0;

    private final IEnergyStorage storage = new WENFeStorage();
    private final LazyOptional<IEnergyStorage> storageOpt = LazyOptional.of(() -> storage);

    public WENFePortBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WEN_FE_PORT.get(), pos, state);
        this.isInput = state.getBlock() == ModBlocks.WEN_FE_INPUT_PORT.get();
    }

    // =========================================================================
    //  Forge Capability
    // =========================================================================

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ENERGY) return storageOpt.cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        storageOpt.invalidate();
    }

    // =========================================================================
    //  NBT
    // =========================================================================

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        this.linkedNetworkId = tag.getString("wen_linked_id");
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putString("wen_linked_id", linkedNetworkId);
    }

    // =========================================================================
    //  Tick — 出力ポートのみ、10tick毎に隣接受信者にpush
    // =========================================================================

    public void serverTick() {
        if (isInput) return;
        if (level == null || level.isClientSide) return;
        if (++tickCounter % TICK_INTERVAL != 0) return;
        if (linkedNetworkId.isEmpty()) return;

        WENNetworkData.WENEntry entry = resolveEntry();
        if (entry == null || entry.storedEnergy.signum() <= 0) return;

        for (Direction dir : Direction.values()) {
            BlockPos npos = worldPosition.relative(dir);
            BlockEntity neighbor = level.getBlockEntity(npos);
            if (neighbor == null) continue;
            neighbor.getCapability(ForgeCapabilities.ENERGY, dir.getOpposite()).ifPresent(sink -> {
                if (!sink.canReceive()) return;
                long feAvailable = entry.storedEnergy
                        .multiply(BigInteger.valueOf(FE_PER_EU))
                        .min(BigInteger.valueOf(MAX_FE_PER_TICK_PUSH))
                        .longValue();
                int feOffer = (int) Math.min(feAvailable, MAX_FE_PER_TICK_PUSH);
                if (feOffer < FE_PER_EU) return;
                int pushed = sink.receiveEnergy(feOffer, false);
                if (pushed >= FE_PER_EU) {
                    long eu = pushed / FE_PER_EU;
                    WENNetworkData data = WENNetworkData.get(level.getServer().overworld());
                    data.removeEnergy(linkedNetworkId, eu);
                }
            });
        }
    }

    // =========================================================================
    //  ID binding
    // =========================================================================

    public String getLinkedNetworkId() { return linkedNetworkId; }

    public void setLinkedNetworkId(String id) {
        this.linkedNetworkId = (id == null) ? "" : id;
        setChanged();
        LOGGER.info("[WEN-FE-Port] {} at {} linked to '{}'",
                isInput ? "INPUT" : "OUTPUT", worldPosition, linkedNetworkId);
    }

    public boolean isInput() { return isInput; }

    public List<String> getValidNetworkIds(WENNetworkData data) {
        if (level == null) return List.of();
        String myDim = level.dimension().location().toString();
        return data.getAllNetworkIds().stream()
                .filter(id -> {
                    WENNetworkData.WENEntry e = data.getNetwork(id);
                    if (e == null || !e.isFormed()) return false;
                    return myDim.equals(e.dimension) || e.crossDimensionEnabled;
                })
                .sorted()
                .collect(Collectors.toList());
    }

    // =========================================================================
    //  WENNetworkData 解決ヘルパ
    // =========================================================================

    @Nullable
    private WENNetworkData.WENEntry resolveEntry() {
        if (level == null || level.isClientSide || level.getServer() == null) return null;
        if (linkedNetworkId.isEmpty()) return null;
        ServerLevel overworld = level.getServer().overworld();
        WENNetworkData data = WENNetworkData.get(overworld);
        WENNetworkData.WENEntry entry = data.getNetwork(linkedNetworkId);
        if (entry == null) return null;
        if (!entry.isFormed()) return null;
        String myDim = level.dimension().location().toString();
        if (!myDim.equals(entry.dimension) && !entry.crossDimensionEnabled) return null;
        return entry;
    }

    // =========================================================================
    //  Forge IEnergyStorage 実装 — WEN ↔ FE 変換
    // =========================================================================

    private class WENFeStorage implements IEnergyStorage {

        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            if (!isInput || maxReceive <= 0) return 0;
            WENNetworkData.WENEntry entry = resolveEntry();
            if (entry == null) return 0;

            BigInteger spaceBig = entry.maxCapacity.subtract(entry.storedEnergy);
            if (spaceBig.signum() <= 0) return 0;

            long spaceEu = spaceBig
                    .min(BigInteger.valueOf(Long.MAX_VALUE / FE_PER_EU))
                    .longValue();
            long feAllowed = Math.min((long) maxReceive, spaceEu * FE_PER_EU);
            feAllowed = (feAllowed / FE_PER_EU) * FE_PER_EU;
            if (feAllowed <= 0) return 0;

            if (!simulate) {
                long euToAdd = feAllowed / FE_PER_EU;
                WENNetworkData data = WENNetworkData.get(level.getServer().overworld());
                long actual = data.addEnergy(linkedNetworkId, euToAdd);
                return (int) (actual * FE_PER_EU);
            }
            return (int) feAllowed;
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            if (isInput || maxExtract <= 0) return 0;
            WENNetworkData.WENEntry entry = resolveEntry();
            if (entry == null || entry.storedEnergy.signum() <= 0) return 0;

            BigInteger feBig = entry.storedEnergy.multiply(BigInteger.valueOf(FE_PER_EU));
            long feAvailable = feBig
                    .min(BigInteger.valueOf(maxExtract))
                    .longValue();
            feAvailable = (feAvailable / FE_PER_EU) * FE_PER_EU;
            if (feAvailable < FE_PER_EU) return 0;

            if (!simulate) {
                long euToRemove = feAvailable / FE_PER_EU;
                WENNetworkData data = WENNetworkData.get(level.getServer().overworld());
                data.removeEnergy(linkedNetworkId, euToRemove);
            }
            return (int) feAvailable;
        }

        @Override
        public int getEnergyStored() {
            if (isInput) return 0;
            WENNetworkData.WENEntry entry = resolveEntry();
            if (entry == null) return 0;
            BigInteger feBig = entry.storedEnergy.multiply(BigInteger.valueOf(FE_PER_EU));
            return feBig.min(BigInteger.valueOf(Integer.MAX_VALUE)).intValue();
        }

        @Override
        public int getMaxEnergyStored() {
            return Integer.MAX_VALUE;
        }

        @Override
        public boolean canExtract() { return !isInput; }

        @Override
        public boolean canReceive() { return isInput; }
    }
}