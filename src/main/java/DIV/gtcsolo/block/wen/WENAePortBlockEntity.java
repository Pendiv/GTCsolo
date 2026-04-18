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
import java.util.stream.Collectors;

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

    private static final IGridNodeListener<WENAePortBlockEntity> NODE_LISTENER = new IGridNodeListener<>() {
        @Override
        public void onSaveChanges(WENAePortBlockEntity owner, IGridNode node) {
            owner.setChanged();
        }
    };

    private final IManagedGridNode mainNode;
    private final boolean isInput;
    private String linkedNetworkId = "";
    private int tickCounter = 0;

    public WENAePortBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WEN_AE_PORT.get(), pos, state);
        this.isInput = state.getBlock() == ModBlocks.WEN_AE_INPUT_PORT.get();
        this.mainNode = GridHelper.createManagedNode(this, NODE_LISTENER)
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
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        mainNode.saveToNBT(tag);
        tag.putString("wen_linked_id", linkedNetworkId);
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
        long spaceEu = spaceBig.min(BigInteger.valueOf(Long.MAX_VALUE / (long) AE_PER_EU)).longValue();
        if (spaceEu <= 0) return;

        double aeWanted = Math.min(spaceEu * AE_PER_EU, MAX_AE_PER_TRANSFER);
        double simExtracted = es.extractAEPower(aeWanted, Actionable.SIMULATE, PowerMultiplier.ONE);
        long euPossible = (long) (simExtracted / AE_PER_EU);
        if (euPossible <= 0) return;

        double aeToPull = euPossible * AE_PER_EU;
        double extracted = es.extractAEPower(aeToPull, Actionable.MODULATE, PowerMultiplier.ONE);
        long euGained = (long) (extracted / AE_PER_EU);
        if (euGained > 0) {
            data.addEnergy(linkedNetworkId, euGained);
        }
    }

    /** Output: WENから取り出して AE2網に注入 */
    private void pushToAe(IEnergyService es, WENNetworkData data, WENNetworkData.WENEntry entry) {
        if (entry.storedEnergy.signum() <= 0) return;

        // AE2網の受入可能量
        double simLeftover = es.injectPower(MAX_AE_PER_TRANSFER, Actionable.SIMULATE);
        double aeAcceptable = MAX_AE_PER_TRANSFER - simLeftover;
        if (aeAcceptable < AE_PER_EU) return;

        long euRequested = (long) (aeAcceptable / AE_PER_EU);
        long euAvailable = entry.storedEnergy.min(BigInteger.valueOf(euRequested)).longValue();
        if (euAvailable <= 0) return;

        double aeToSend = euAvailable * AE_PER_EU;
        double finalLeftover = es.injectPower(aeToSend, Actionable.MODULATE);
        long euSent = (long) ((aeToSend - finalLeftover) / AE_PER_EU);
        if (euSent > 0) {
            data.removeEnergy(linkedNetworkId, euSent);
        }
    }

    // =========================================================================
    //  ID binding
    // =========================================================================

    public String getLinkedNetworkId() { return linkedNetworkId; }

    public void setLinkedNetworkId(String id) {
        this.linkedNetworkId = (id == null) ? "" : id;
        setChanged();
        LOGGER.info("[WEN-AE-Port] {} at {} linked to '{}'",
                isInput ? "INPUT" : "OUTPUT", worldPosition, linkedNetworkId);
    }

    public boolean isInput() { return isInput; }

    /** 同一次元またはcrossDim許可の網のみ */
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
}