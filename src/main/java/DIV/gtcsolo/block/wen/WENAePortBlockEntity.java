package DIV.gtcsolo.block.wen;

import DIV.gtcsolo.machine.wen.WENNetworkData;
import DIV.gtcsolo.registry.ModBlockEntities;
import DIV.gtcsolo.registry.ModBlocks;
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
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.math.BigInteger;
import java.util.EnumSet;
import java.util.List;

/**
 * AE2版 WEN Input/Output Port の BlockEntity。
 * AE2グリッドノードとして動作し、WEN網との間でエネルギーを変換 (1 EU = 2 AE)。
 * 10tick毎に転送。Shift+右クリックで既知ID選択UIを開く。
 */
public class WENAePortBlockEntity extends BlockEntity implements IInWorldGridNodeHost {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static final double AE_PER_EU = 2.0;
    private static final double MAX_AE_PER_TRANSFER = 1_000_000_000.0;
    private static final int TRANSFER_INTERVAL = 10; // ticks

    private static final IGridNodeListener<WENAePortBlockEntity> NODE_LISTENER =
            new IGridNodeListener<>() {
                @Override
                public void onSaveChanges(WENAePortBlockEntity owner, IGridNode node) {
                    owner.setChanged();
                }
            };

    private final IManagedGridNode mainNode;
    private final boolean isInput;
    private String linkedNetworkId = "";
    private int tickCounter = 0;
    private double prepaidOutputAe;
    private double pendingInputAe;

    public WENAePortBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WEN_AE_PORT.get(), pos, state);
        this.isInput = state.getBlock() == ModBlocks.WEN_AE_INPUT_PORT.get();
        this.mainNode =
                GridHelper.createManagedNode(this, NODE_LISTENER)
                        .setInWorldNode(true)
                        .setTagName("proxy")
                        .setExposedOnSides(EnumSet.allOf(Direction.class))
                        .setIdlePowerUsage(0.0);
    }

    // =========================================================================
    //  IInWorldGridNodeHost
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
    //  Lifecycle
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
        this.linkedNetworkId = tag.getString("wen_linked_id");
        prepaidOutputAe = validRemainder(tag.getDouble("wen_prepaid_ae"));
        pendingInputAe = validRemainder(tag.getDouble("wen_pending_ae"));
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        mainNode.saveToNBT(tag);
        tag.putString("wen_linked_id", linkedNetworkId);
        tag.putDouble("wen_prepaid_ae", prepaidOutputAe);
        tag.putDouble("wen_pending_ae", pendingInputAe);
    }

    // =========================================================================
    //  Tick (Block.getTicker から呼ばれる、サーバー側のみ)
    // =========================================================================

    public void serverTick() {
        if (level == null || level.isClientSide) return;
        if (++tickCounter % TRANSFER_INTERVAL != 0) return;
        if (linkedNetworkId.isEmpty()) return;

        IGrid grid = mainNode.getGrid();
        if (grid == null) return;
        IEnergyService es = grid.getEnergyService();

        ServerLevel overworld = level.getServer().overworld();
        WENNetworkData data = WENNetworkData.get(overworld);
        WENNetworkData.WENEntry entry = data.getNetwork(linkedNetworkId);
        if (entry == null) {
            linkedNetworkId = ""; // 存在しないID → 自動unlink
            setChanged();
            return;
        }
        if (!entry.isFormed()) return;

        // ディメンション制限
        String myDim = level.dimension().location().toString();
        if (!myDim.equals(entry.dimension) && !entry.crossDimensionEnabled) return;

        if (isInput) {
            pullFromAe(es, data, entry);
        } else {
            pushToAe(es, data, entry);
        }
    }

    /** Input: AE2網から吸い上げてWENに格納 */
    private void pullFromAe(IEnergyService es, WENNetworkData data, WENNetworkData.WENEntry entry) {
        if (!es.isNetworkPowered()) return;

        // WEN空き容量を EU で算出
        BigInteger spaceBig = entry.maxCapacity.subtract(entry.storedEnergy);
        if (spaceBig.signum() <= 0) return;
        long spaceEu =
                spaceBig.min(BigInteger.valueOf(Long.MAX_VALUE / (long) AE_PER_EU)).longValue();
        if (spaceEu <= 0) return;

        double aeWanted =
                DIV.gtcsolo.util.EnergyMath.receivableInput(
                        spaceEu, pendingInputAe, AE_PER_EU, MAX_AE_PER_TRANSFER);
        double simExtracted = es.extractAEPower(aeWanted, Actionable.SIMULATE, PowerMultiplier.ONE);
        if (!Double.isFinite(simExtracted) || simExtracted <= 0) return;
        double aeToPull = Math.min(aeWanted, simExtracted);
        double extracted = es.extractAEPower(aeToPull, Actionable.MODULATE, PowerMultiplier.ONE);
        if (!Double.isFinite(extracted) || extracted <= 0) return;
        extracted = Math.min(extracted, aeToPull);
        var settlement =
                DIV.gtcsolo.util.EnergyMath.settleInput(extracted, pendingInputAe, AE_PER_EU);
        long euGained = settlement.eu();
        pendingInputAe = validRemainder(settlement.pending());
        if (extracted > 0) setChanged();
        if (euGained > 0) {
            data.addEnergy(linkedNetworkId, euGained);
        }
    }

    /** Output: WENから取り出して AE2網に注入 */
    private void pushToAe(IEnergyService es, WENNetworkData data, WENNetworkData.WENEntry entry) {
        if (entry.storedEnergy.signum() <= 0 && prepaidOutputAe <= 0) return;

        // AE2網の受入可能量
        double simLeftover = es.injectPower(MAX_AE_PER_TRANSFER, Actionable.SIMULATE);
        double aeAcceptable = MAX_AE_PER_TRANSFER - simLeftover;
        if (!Double.isFinite(aeAcceptable) || aeAcceptable <= 0) return;

        long euRequested = (long) Math.ceil(aeAcceptable / AE_PER_EU);
        long euAvailable = entry.storedEnergy.min(BigInteger.valueOf(euRequested)).longValue();
        double aeToSend =
                Math.min(
                        aeAcceptable,
                        DIV.gtcsolo.util.EnergyMath.payableOutput(
                                euAvailable, prepaidOutputAe, AE_PER_EU));
        if (aeToSend <= 0) return;
        double finalLeftover = es.injectPower(aeToSend, Actionable.MODULATE);
        double accepted = Math.max(0, Math.min(aeToSend, aeToSend - finalLeftover));
        if (accepted > 0) {
            var settlement =
                    DIV.gtcsolo.util.EnergyMath.settleOutput(accepted, prepaidOutputAe, AE_PER_EU);
            data.removeEnergy(linkedNetworkId, settlement.eu());
            prepaidOutputAe = settlement.prepaid();
            setChanged();
        }
    }

    // =========================================================================
    //  ID binding
    // =========================================================================

    private static double validRemainder(double value) {
        return Double.isFinite(value) && value >= 0 && value <= AE_PER_EU ? value : 0;
    }

    public String getLinkedNetworkId() {
        return linkedNetworkId;
    }

    public void setLinkedNetworkId(String id) {
        this.linkedNetworkId = (id == null) ? "" : id;
        setChanged();
        LOGGER.debug(
                "[WEN-AE-Port] {} at {} linked to '{}'",
                isInput ? "INPUT" : "OUTPUT",
                worldPosition,
                linkedNetworkId);
    }

    public boolean isInput() {
        return isInput;
    }

    /** 同一次元またはcrossDim許可の網のみ */
    public List<String> getValidNetworkIds(WENNetworkData data) {
        return level == null
                ? List.of()
                : data.getAccessibleNetworkIds(level.dimension().location().toString());
    }
}
