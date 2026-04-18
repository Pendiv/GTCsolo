package DIV.gtcsolo.block.wen;

import DIV.gtcsolo.registry.ModBlockEntities;
import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.networking.GridHelper;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IGridNodeListener;
import appeng.api.networking.IInWorldGridNodeHost;
import appeng.api.networking.IManagedGridNode;
import appeng.api.networking.energy.IEnergyService;
import appeng.api.util.AECableType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

/**
 * WEN Input/Output PortのBlockEntity。
 * FE Capability を公開してFEケーブル接続受付、AE2グリッドノードも保持してAE2ケーブル受付。
 * 実際のEU転送はWENMainStorageMachineのtickが行う。
 */
public class WENPortBlockEntity extends BlockEntity implements IInWorldGridNodeHost {

    /** シンプルなGridNodeListener — NBTダーティ通知のみ */
    private static final IGridNodeListener<WENPortBlockEntity> NODE_LISTENER = new IGridNodeListener<>() {
        @Override
        public void onSaveChanges(WENPortBlockEntity owner, IGridNode node) {
            owner.setChanged();
        }
    };

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

    /** AE2 グリッドノード — 全面exposeで、AE2ケーブルが接続できるようにする */
    private final IManagedGridNode mainNode;

    public WENPortBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WEN_PORT.get(), pos, state);
        this.isInput = state.getBlock() == DIV.gtcsolo.registry.ModBlocks.WEN_MAINSTORAGE_INPUT_PORT.get();
        this.mainNode = GridHelper.createManagedNode(this, NODE_LISTENER)
                .setInWorldNode(true)
                .setTagName("proxy")
                .setExposedOnSides(EnumSet.allOf(Direction.class))
                .setIdlePowerUsage(0.0);
    }

    // =========================================================================
    //  Forge Capability (FE)
    // =========================================================================

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

    // =========================================================================
    //  AE2 IInWorldGridNodeHost
    // =========================================================================

    @Override
    public @Nullable IGridNode getGridNode(@NotNull Direction dir) {
        return mainNode.getNode();
    }

    @Override
    public @NotNull AECableType getCableConnectionType(@NotNull Direction dir) {
        return AECableType.SMART;
    }

    // =========================================================================
    //  Grid node ライフサイクル
    // =========================================================================

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) {
            mainNode.create(level, worldPosition);
        }
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        mainNode.destroy();
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        mainNode.destroy();
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        mainNode.loadFromNBT(tag);
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        mainNode.saveToNBT(tag);
    }

    // =========================================================================
    //  AE エネルギー操作ヘルパ — WENMainStorageMachine から呼ばれる
    // =========================================================================

    /**
     * AE網から最大 maxAE を取得し、実際に取れた量を返す (MODULATE)。
     * グリッド未接続/電源未供給なら 0。
     */
    public double extractAEPower(double maxAE) {
        if (maxAE <= 0) return 0;
        IEnergyService es = getEnergyService();
        if (es == null || !es.isNetworkPowered()) return 0;
        return es.extractAEPower(maxAE, Actionable.MODULATE, PowerMultiplier.ONE);
    }

    /** SIMULATEで抽出可能量を試算 */
    public double simulateExtractAE(double maxAE) {
        if (maxAE <= 0) return 0;
        IEnergyService es = getEnergyService();
        if (es == null || !es.isNetworkPowered()) return 0;
        return es.extractAEPower(maxAE, Actionable.SIMULATE, PowerMultiplier.ONE);
    }

    /**
     * AE網に amt を注入し、受入れ不能で残った量を返す (MODULATE)。
     * グリッド未接続なら全量 (amt) が残り扱い。
     */
    public double injectAEPower(double amt) {
        if (amt <= 0) return 0;
        IEnergyService es = getEnergyService();
        if (es == null) return amt;
        return es.injectPower(amt, Actionable.MODULATE);
    }

    /** SIMULATEで注入可能量を試算 */
    public double simulateInjectAE(double amt) {
        if (amt <= 0) return 0;
        IEnergyService es = getEnergyService();
        if (es == null) return amt;
        return es.injectPower(amt, Actionable.SIMULATE);
    }

    @Nullable
    private IEnergyService getEnergyService() {
        IGrid grid = mainNode.getGrid();
        if (grid == null) return null;
        return grid.getService(IEnergyService.class);
    }

    public boolean isInput() {
        return isInput;
    }
}
