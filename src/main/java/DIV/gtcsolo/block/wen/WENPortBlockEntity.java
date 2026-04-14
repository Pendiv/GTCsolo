package DIV.gtcsolo.block.wen;

import DIV.gtcsolo.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * WEN Input/Output PortのBlockEntity。
 * FE Capabilityを公開してFEケーブルの接続を受ける。
 * 実際のエネルギー転送はWENMainStorageMachineのtickが行う。
 * このBEはCapabilityのダミー公開のみ。
 */
public class WENPortBlockEntity extends BlockEntity {

    private final boolean isInput;

    /** ダミーFEストレージ: ケーブル接続を許可するためだけに存在 */
    private final IEnergyStorage feStorage = new IEnergyStorage() {
        @Override public int receiveEnergy(int max, boolean sim) { return 0; }
        @Override public int extractEnergy(int max, boolean sim) { return 0; }
        @Override public int getEnergyStored() { return 0; }
        @Override public int getMaxEnergyStored() { return Integer.MAX_VALUE; }
        @Override public boolean canExtract() { return !isInput; }
        @Override public boolean canReceive() { return isInput; }
    };

    private final LazyOptional<IEnergyStorage> feOpt = LazyOptional.of(() -> feStorage);

    public WENPortBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WEN_PORT.get(), pos, state);
        // ブロックの種類で入力/出力を判定
        this.isInput = state.getBlock() == DIV.gtcsolo.registry.ModBlocks.WEN_MAINSTORAGE_INPUT_PORT.get();
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ENERGY) return feOpt.cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        feOpt.invalidate();
    }
}
