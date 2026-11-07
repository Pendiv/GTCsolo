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

/**
 * FE Wireless Port BE: Forge Energy ↔ WEN 双方向変換 (1 EU = 4 FE)。
 * Capability経由でFEケーブル/機械と直接やり取り + 出力ポートは10tick毎に隣接受信者に push。
 */
public class WENFePortBlockEntity extends BlockEntity {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static final int FE_PER_EU = 4;
    private static final int MAX_FE_PER_TRANSFER = 1_000_000_000;
    private static final Direction[] SIDES = Direction.values();
    private static final int TICK_INTERVAL = 10;

    private final boolean isInput;
    private String linkedNetworkId = "";
    private int tickCounter = 0;
    private int prepaidOutputFe;
    private int pendingInputFe;

    private final IEnergyStorage storage = new WENFeStorage();
    private LazyOptional<IEnergyStorage> storageOpt = LazyOptional.of(() -> storage);

    public WENFePortBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WEN_FE_PORT.get(), pos, state);
        this.isInput = state.getBlock() == ModBlocks.WEN_FE_INPUT_PORT.get();
    }

    // =========================================================================
    //  Forge Capability
    // =========================================================================

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(
            @NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ENERGY) return storageOpt.cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        storageOpt.invalidate();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        storageOpt = LazyOptional.of(() -> storage);
    }

    // =========================================================================
    //  NBT
    // =========================================================================

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        this.linkedNetworkId = tag.getString("wen_linked_id");
        prepaidOutputFe = Math.max(0, Math.min(FE_PER_EU - 1, tag.getInt("wen_prepaid_fe")));
        pendingInputFe = Math.max(0, Math.min(FE_PER_EU - 1, tag.getInt("wen_pending_fe")));
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putString("wen_linked_id", linkedNetworkId);
        tag.putInt("wen_prepaid_fe", prepaidOutputFe);
        tag.putInt("wen_pending_fe", pendingInputFe);
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
        if (entry == null || (entry.storedEnergy.signum() <= 0 && prepaidOutputFe == 0)) return;

        for (Direction dir : SIDES) {
            BlockPos npos = worldPosition.relative(dir);
            if (!level.hasChunkAt(npos)) continue;
            BlockEntity neighbor = level.getBlockEntity(npos);
            if (neighbor == null) continue;
            neighbor.getCapability(ForgeCapabilities.ENERGY, dir.getOpposite())
                    .ifPresent(
                            sink -> {
                                if (!sink.canReceive()) return;
                                int feOffer = availableOutput(entry, MAX_FE_PER_TRANSFER);
                                if (feOffer <= 0) return;
                                int pushed =
                                        Math.max(
                                                0,
                                                Math.min(
                                                        feOffer,
                                                        sink.receiveEnergy(feOffer, false)));
                                if (pushed > 0) {
                                    var settlement =
                                            DIV.gtcsolo.util.EnergyMath.settleOutput(
                                                    pushed, prepaidOutputFe, FE_PER_EU);
                                    WENNetworkData data =
                                            WENNetworkData.get(level.getServer().overworld());
                                    data.removeEnergy(linkedNetworkId, settlement.eu());
                                    prepaidOutputFe = (int) settlement.prepaid();
                                    setChanged();
                                }
                            });
        }
    }

    // =========================================================================
    //  ID binding
    // =========================================================================

    public String getLinkedNetworkId() {
        return linkedNetworkId;
    }

    public void setLinkedNetworkId(String id) {
        this.linkedNetworkId = (id == null) ? "" : id;
        setChanged();
        LOGGER.debug(
                "[WEN-FE-Port] {} at {} linked to '{}'",
                isInput ? "INPUT" : "OUTPUT",
                worldPosition,
                linkedNetworkId);
    }

    public boolean isInput() {
        return isInput;
    }

    public List<String> getValidNetworkIds(WENNetworkData data) {
        return level == null
                ? List.of()
                : data.getAccessibleNetworkIds(level.dimension().location().toString());
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
        return data.accessibleNetwork(linkedNetworkId, level.dimension().location().toString());
    }

    private int availableOutput(WENNetworkData.WENEntry entry, int limit) {
        long eu = entry.storedEnergy.min(BigInteger.valueOf((limit + 3L) / FE_PER_EU)).longValue();
        return (int) Math.min(limit, eu * FE_PER_EU + prepaidOutputFe);
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

            long spaceEu = spaceBig.min(BigInteger.valueOf(Long.MAX_VALUE / FE_PER_EU)).longValue();
            int accepted =
                    (int)
                            Math.min(
                                    (long) maxReceive,
                                    Math.max(0, spaceEu * FE_PER_EU - pendingInputFe));
            if (accepted <= 0) return 0;
            if (!simulate) {
                var settlement =
                        DIV.gtcsolo.util.EnergyMath.settleInput(
                                accepted, pendingInputFe, FE_PER_EU);
                WENNetworkData.get(level.getServer().overworld())
                        .addEnergy(linkedNetworkId, settlement.eu());
                pendingInputFe = (int) settlement.pending();
                setChanged();
            }
            return accepted;
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            if (isInput || maxExtract <= 0) return 0;
            var entry = resolveEntry();
            if (entry == null) return 0;
            int extracted = availableOutput(entry, maxExtract);
            if (extracted > 0 && !simulate) {
                var settlement =
                        DIV.gtcsolo.util.EnergyMath.settleOutput(
                                extracted, prepaidOutputFe, FE_PER_EU);
                WENNetworkData.get(level.getServer().overworld())
                        .removeEnergy(linkedNetworkId, settlement.eu());
                prepaidOutputFe = (int) settlement.prepaid();
                setChanged();
            }
            return extracted;
        }

        @Override
        public int getEnergyStored() {
            if (isInput) return pendingInputFe;
            var entry = resolveEntry();
            return entry == null ? 0 : availableOutput(entry, Integer.MAX_VALUE);
        }

        @Override
        public int getMaxEnergyStored() {
            return Integer.MAX_VALUE;
        }

        @Override
        public boolean canExtract() {
            return !isInput;
        }

        @Override
        public boolean canReceive() {
            return isInput;
        }
    }
}
