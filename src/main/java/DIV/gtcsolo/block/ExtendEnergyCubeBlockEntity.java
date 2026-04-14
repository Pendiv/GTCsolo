package DIV.gtcsolo.block;

import DIV.gtcsolo.network.EnergyCubeUpdatePacket;
import DIV.gtcsolo.network.ModNetwork;
import DIV.gtcsolo.registry.ModBlockEntities;
import com.gregtechceu.gtceu.api.capability.forge.GTCapability;
import com.gregtechceu.gtceu.api.capability.GTCapabilityHelper;
import com.gregtechceu.gtceu.api.capability.IEnergyContainer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 拡張型蓄電器 v2
 * - 内部はFE(long)で保持、EU変換は 4FE = 1EU
 * - UIスイッチ: 吸収/放出、FE/EU切替
 * - EU出力時は隣接GTブロックの電圧を自動検出
 */
public class ExtendEnergyCubeBlockEntity extends BlockEntity implements MenuProvider {

    public static final long MAX_CAPACITY = Long.MAX_VALUE; // 2^63-1
    public static final int FE_PER_EU = 4;

    /** 内部エネルギー (FE単位) */
    private long storedEnergy = 0;

    /** true = 放出モード, false = 吸収モード */
    private boolean emitMode = false;

    /** true = EU出力, false = FE出力 (放出モード時のみ意味がある) */
    private boolean euMode = false;

    private int syncTimer = 0;

    // ---- Forge Energy Capability (FE入出力用) ----

    private final IEnergyStorage feCapability = new IEnergyStorage() {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            if (emitMode || maxReceive <= 0) return 0;
            long space = MAX_CAPACITY - storedEnergy;
            if (space <= 0) return 0;
            int actual = (int) Math.min(maxReceive, Math.min(space, Integer.MAX_VALUE));
            if (!simulate && actual > 0) {
                storedEnergy += actual;
                setChanged();
            }
            return actual;
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            if (!emitMode || euMode || maxExtract <= 0 || storedEnergy <= 0) return 0;
            int actual = (int) Math.min(maxExtract, Math.min(storedEnergy, Integer.MAX_VALUE));
            if (!simulate && actual > 0) {
                storedEnergy -= actual;
                setChanged();
            }
            return actual;
        }

        @Override
        public int getEnergyStored() {
            return (int) Math.min(storedEnergy, Integer.MAX_VALUE);
        }

        @Override
        public int getMaxEnergyStored() {
            return Integer.MAX_VALUE;
        }

        @Override
        public boolean canExtract() { return emitMode && !euMode; }

        @Override
        public boolean canReceive() { return !emitMode; }
    };

    // ---- GT Energy Capability (EU入出力用) ----

    private final IEnergyContainer euCapability = new IEnergyContainer() {
        @Override
        public long acceptEnergyFromNetwork(Direction side, long voltage, long amperage) {
            if (emitMode) return 0;
            long spaceInFe = MAX_CAPACITY - storedEnergy;
            long spaceInEu = spaceInFe / FE_PER_EU;
            if (spaceInEu <= 0) return 0;

            long maxAmps = Math.min(amperage, spaceInEu / Math.max(voltage, 1));
            if (maxAmps <= 0) return 0;

            long euAccepted = maxAmps * voltage;
            storedEnergy += euAccepted * FE_PER_EU;
            setChanged();
            return maxAmps;
        }

        @Override
        public boolean inputsEnergy(Direction side) {
            return !emitMode;
        }

        @Override
        public boolean outputsEnergy(Direction side) {
            return false; // EU出力はtickで能動的に行う
        }

        @Override
        public long changeEnergy(long differenceAmount) {
            long feChange = differenceAmount * FE_PER_EU;
            long before = storedEnergy;
            storedEnergy = Math.max(0, Math.min(MAX_CAPACITY, storedEnergy + feChange));
            setChanged();
            return (storedEnergy - before) / FE_PER_EU;
        }

        @Override
        public long getEnergyStored() {
            return storedEnergy / FE_PER_EU;
        }

        @Override
        public long getEnergyCapacity() {
            return MAX_CAPACITY / FE_PER_EU;
        }

        @Override
        public long getInputAmperage() { return 16; }

        @Override
        public long getInputVoltage() {
            return com.gregtechceu.gtceu.api.GTValues.V[com.gregtechceu.gtceu.api.GTValues.MAX];
        }
    };

    private final LazyOptional<IEnergyStorage> feOptional = LazyOptional.of(() -> feCapability);
    private final LazyOptional<IEnergyContainer> euOptional = LazyOptional.of(() -> euCapability);

    public ExtendEnergyCubeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.EXTEND_ENERGY_CUBE.get(), pos, state);
    }

    // =========================================================================
    //  Tick
    // =========================================================================

    public static void tick(Level level, BlockPos pos, BlockState state, ExtendEnergyCubeBlockEntity be) {
        if (level.isClientSide) return;

        if (be.emitMode) {
            if (be.euMode) {
                be.pushEuToNeighbors(level, pos);
            } else {
                be.pushFeToNeighbors(level, pos);
            }
        } else {
            be.pullFeFromNeighbors(level, pos);
            // EU入力はGTケーブル側がacceptEnergyFromNetworkを呼ぶので能動的pullは不要
        }

        // 20tick (1秒) ごとにUI同期
        be.syncTimer++;
        if (be.syncTimer >= 20) {
            be.syncTimer = 0;
            be.syncToViewers(level, pos);
        }
    }

    // ---- FE出力 ----

    private void pushFeToNeighbors(Level level, BlockPos pos) {
        if (storedEnergy <= 0) return;
        for (Direction dir : Direction.values()) {
            BlockEntity neighbor = level.getBlockEntity(pos.relative(dir));
            if (neighbor == null) continue;
            neighbor.getCapability(ForgeCapabilities.ENERGY, dir.getOpposite()).ifPresent(storage -> {
                if (!storage.canReceive()) return;
                int toSend = (int) Math.min(storedEnergy, Integer.MAX_VALUE);
                int accepted = storage.receiveEnergy(toSend, false);
                if (accepted > 0) {
                    storedEnergy -= accepted;
                    setChanged();
                }
            });
            if (storedEnergy <= 0) break;
        }
    }

    // ---- EU出力 (隣接GTブロックの電圧を自動検出) ----

    private void pushEuToNeighbors(Level level, BlockPos pos) {
        if (storedEnergy < FE_PER_EU) return;
        for (Direction dir : Direction.values()) {
            Direction opposite = dir.getOpposite();
            IEnergyContainer neighbor = GTCapabilityHelper.getEnergyContainer(
                    level, pos.relative(dir), opposite);
            if (neighbor == null || !neighbor.inputsEnergy(opposite)) continue;

            long voltage = neighbor.getInputVoltage();
            long amperage = neighbor.getInputAmperage();
            if (voltage <= 0 || amperage <= 0) continue;

            // 送れる最大アンペア数 = min(相手の受入アンペア, 蓄電量から出せる量)
            long availableEu = storedEnergy / FE_PER_EU;
            long maxAmps = Math.min(amperage, availableEu / voltage);
            if (maxAmps <= 0) continue;

            long ampsAccepted = neighbor.acceptEnergyFromNetwork(opposite, voltage, maxAmps);
            if (ampsAccepted > 0) {
                long euSent = ampsAccepted * voltage;
                storedEnergy -= euSent * FE_PER_EU;
                setChanged();
            }
            if (storedEnergy < FE_PER_EU) break;
        }
    }

    // ---- FE吸収 ----

    private void pullFeFromNeighbors(Level level, BlockPos pos) {
        if (storedEnergy >= MAX_CAPACITY) return;
        for (Direction dir : Direction.values()) {
            BlockEntity neighbor = level.getBlockEntity(pos.relative(dir));
            if (neighbor == null) continue;
            neighbor.getCapability(ForgeCapabilities.ENERGY, dir.getOpposite()).ifPresent(storage -> {
                if (!storage.canExtract()) return;
                long space = MAX_CAPACITY - storedEnergy;
                int toRequest = (int) Math.min(space, Integer.MAX_VALUE);
                int extracted = storage.extractEnergy(toRequest, false);
                if (extracted > 0) {
                    storedEnergy += extracted;
                    setChanged();
                }
            });
            if (storedEnergy >= MAX_CAPACITY) break;
        }
    }

    // =========================================================================
    //  スイッチ操作
    // =========================================================================

    public boolean isEmitMode() { return emitMode; }
    public boolean isEuMode() { return euMode; }
    public long getStoredEnergy() { return storedEnergy; }

    public void setEmitMode(boolean emit) {
        this.emitMode = emit;
        setChanged();
    }

    public void setEuMode(boolean eu) {
        this.euMode = eu;
        setChanged();
    }

    // =========================================================================
    //  UI同期
    // =========================================================================

    private void syncToViewers(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        EnergyCubeUpdatePacket packet = new EnergyCubeUpdatePacket(
                pos, storedEnergy, emitMode, euMode);
        for (ServerPlayer player : serverLevel.getServer().getPlayerList().getPlayers()) {
            if (player.containerMenu instanceof ExtendEnergyCubeMenu menu
                    && menu.getBlockPos().equals(pos)) {
                ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
            }
        }
    }

    // =========================================================================
    //  MenuProvider
    // =========================================================================

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.gtcsolo.extend_energy_cube");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new ExtendEnergyCubeMenu(id, worldPosition, storedEnergy, emitMode, euMode);
    }

    public void writeScreenOpenData(FriendlyByteBuf buf) {
        buf.writeBlockPos(worldPosition);
        buf.writeLong(storedEnergy);
        buf.writeBoolean(emitMode);
        buf.writeBoolean(euMode);
    }

    // =========================================================================
    //  Capability
    // =========================================================================

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ENERGY) return feOptional.cast();
        if (cap == GTCapability.CAPABILITY_ENERGY_CONTAINER) return euOptional.cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        feOptional.invalidate();
        euOptional.invalidate();
    }

    // =========================================================================
    //  NBT
    // =========================================================================

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        storedEnergy = tag.getLong("Energy");
        if (storedEnergy < 0) storedEnergy = 0;
        emitMode = tag.getBoolean("EmitMode");
        euMode = tag.getBoolean("EuMode");
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putLong("Energy", storedEnergy);
        tag.putBoolean("EmitMode", emitMode);
        tag.putBoolean("EuMode", euMode);
    }
}