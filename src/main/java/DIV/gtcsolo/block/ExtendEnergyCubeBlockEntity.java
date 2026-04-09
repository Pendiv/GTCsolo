package DIV.gtcsolo.block;

import DIV.gtcsolo.network.EnergyCubeUpdatePacket;
import DIV.gtcsolo.network.ModNetwork;
import DIV.gtcsolo.registry.ModBlockEntities;
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

import java.math.BigInteger;

public class ExtendEnergyCubeBlockEntity extends BlockEntity implements MenuProvider {

    // 2^128 - 1
    public static final BigInteger MAX_CAPACITY =
            BigInteger.ONE.shiftLeft(128).subtract(BigInteger.ONE);
    // 搬入速度: 2^64 - 1
    public static final BigInteger INPUT_RATE =
            BigInteger.ONE.shiftLeft(64).subtract(BigInteger.ONE);
    // 搬出速度(通常): 2^32 - 1
    public static final BigInteger NORMAL_OUTPUT_RATE =
            BigInteger.ONE.shiftLeft(32).subtract(BigInteger.ONE);
    // 搬出速度(BOOST時): 2^64 - 1
    public static final BigInteger BOOSTED_OUTPUT_RATE =
            BigInteger.ONE.shiftLeft(64).subtract(BigInteger.ONE);

    private BigInteger storedEnergy = BigInteger.ZERO;
    private int syncTimer = 0;

    // Forge Energy Capability ラッパー
    // IEnergyStorage は int ベースのため、実際の転送量は Integer.MAX_VALUE が上限
    private final IEnergyStorage energyCapability = new IEnergyStorage() {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            if (maxReceive <= 0) return 0;
            BigInteger space = MAX_CAPACITY.subtract(storedEnergy);
            if (space.signum() <= 0) return 0;
            int actual = (int) Math.min(maxReceive,
                    space.min(BigInteger.valueOf(Integer.MAX_VALUE)).longValue());
            if (!simulate && actual > 0) {
                storedEnergy = storedEnergy.add(BigInteger.valueOf(actual));
                setChanged();
            }
            return actual;
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            if (maxExtract <= 0 || storedEnergy.signum() == 0) return 0;
            int actual = (int) Math.min(maxExtract,
                    storedEnergy.min(BigInteger.valueOf(Integer.MAX_VALUE)).longValue());
            if (!simulate && actual > 0) {
                storedEnergy = storedEnergy.subtract(BigInteger.valueOf(actual));
                setChanged();
            }
            return actual;
        }

        @Override
        public int getEnergyStored() {
            return storedEnergy.min(BigInteger.valueOf(Integer.MAX_VALUE)).intValue();
        }

        @Override
        public int getMaxEnergyStored() {
            return Integer.MAX_VALUE;
        }

        @Override
        public boolean canExtract() { return true; }

        @Override
        public boolean canReceive() { return true; }
    };

    private final LazyOptional<IEnergyStorage> energyOptional = LazyOptional.of(() -> energyCapability);

    public ExtendEnergyCubeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.EXTEND_ENERGY_CUBE.get(), pos, state);
    }

    // --- Tick ---

    public static void tick(Level level, BlockPos pos, BlockState state, ExtendEnergyCubeBlockEntity be) {
        if (level.isClientSide) return;

        boolean powered = state.getValue(ExtendEnergyCubeBlock.POWERED);
        if (powered) {
            be.pushEnergyToNeighbors(level, pos, state);
        } else {
            be.pullEnergyFromNeighbors(level, pos);
        }

        // 20tick (1秒) ごとに開いているUIへ最新データを送信
        be.syncTimer++;
        if (be.syncTimer >= 20) {
            be.syncTimer = 0;
            be.syncToViewers(level, pos, state);
        }
    }

    /** OUTPUT モード: 隣接ブロックに FE を押し出す */
    private void pushEnergyToNeighbors(Level level, BlockPos pos, BlockState state) {
        if (storedEnergy.signum() == 0) return;
        for (Direction dir : Direction.values()) {
            BlockEntity neighbor = level.getBlockEntity(pos.relative(dir));
            if (neighbor == null) continue;
            neighbor.getCapability(ForgeCapabilities.ENERGY, dir.getOpposite()).ifPresent(storage -> {
                if (!storage.canReceive()) return;
                int toSend = storedEnergy.min(BigInteger.valueOf(Integer.MAX_VALUE)).intValue();
                int accepted = storage.receiveEnergy(toSend, false);
                if (accepted > 0) {
                    storedEnergy = storedEnergy.subtract(BigInteger.valueOf(accepted));
                    setChanged();
                }
            });
            if (storedEnergy.signum() == 0) break;
        }
    }

    /** ABSORB モード: 隣接ブロックから FE を引き抜く */
    private void pullEnergyFromNeighbors(Level level, BlockPos pos) {
        if (storedEnergy.compareTo(MAX_CAPACITY) >= 0) return;
        for (Direction dir : Direction.values()) {
            BlockEntity neighbor = level.getBlockEntity(pos.relative(dir));
            if (neighbor == null) continue;
            neighbor.getCapability(ForgeCapabilities.ENERGY, dir.getOpposite()).ifPresent(storage -> {
                if (!storage.canExtract()) return;
                BigInteger space = MAX_CAPACITY.subtract(storedEnergy);
                int toRequest = space.min(BigInteger.valueOf(Integer.MAX_VALUE)).intValue();
                int extracted = storage.extractEnergy(toRequest, false);
                if (extracted > 0) {
                    storedEnergy = storedEnergy.add(BigInteger.valueOf(extracted));
                    setChanged();
                }
            });
            if (storedEnergy.compareTo(MAX_CAPACITY) >= 0) break;
        }
    }

    /** このブロックのメニューを開いているプレイヤーへ最新データを送信する */
    private void syncToViewers(Level level, BlockPos pos, BlockState state) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        EnergyCubeUpdatePacket packet = new EnergyCubeUpdatePacket(
                pos, storedEnergy, currentOutputRate(state));
        for (ServerPlayer player : serverLevel.getServer().getPlayerList().getPlayers()) {
            if (player.containerMenu instanceof ExtendEnergyCubeMenu menu
                    && menu.getBlockPos().equals(pos)) {
                ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
            }
        }
    }

    // --- MenuProvider ---

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.gtcsolo.extend_energy_cube");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new ExtendEnergyCubeMenu(id, worldPosition, storedEnergy, currentOutputRate(getBlockState()));
    }

    /** NetworkHooks.openScreen の extraData 書き込み */
    public void writeScreenOpenData(FriendlyByteBuf buf) {
        buf.writeBlockPos(worldPosition);
        buf.writeUtf(storedEnergy.toString());
        buf.writeUtf(currentOutputRate(getBlockState()).toString());
    }

    private BigInteger currentOutputRate(BlockState state) {
        boolean boosted = state.getValue(ExtendEnergyCubeBlock.BOOSTED);
        return boosted ? BOOSTED_OUTPUT_RATE : NORMAL_OUTPUT_RATE;
    }

    // --- Capability ---

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ENERGY) return energyOptional.cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        energyOptional.invalidate();
    }

    // --- NBT ---

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("Energy")) {
            storedEnergy = new BigInteger(tag.getByteArray("Energy"));
            if (storedEnergy.signum() < 0) storedEnergy = BigInteger.ZERO;
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putByteArray("Energy", storedEnergy.toByteArray());
    }
}