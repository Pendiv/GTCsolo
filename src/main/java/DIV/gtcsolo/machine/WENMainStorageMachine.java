package DIV.gtcsolo.machine;

import DIV.gtcsolo.block.wen.WENDataMonitorBlockEntity;
import DIV.gtcsolo.machine.wen.WENNetworkData;
import DIV.gtcsolo.machine.wen.WENStorageStructure;
import DIV.gtcsolo.registry.ModBlocks;
import DIV.gtcsolo.registry.ModItems;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.TickableSubscription;
import com.gregtechceu.gtceu.api.machine.feature.IDropSaveMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.pattern.BlockPattern;
import com.gregtechceu.gtceu.api.pattern.error.PatternStringError;
import com.gregtechceu.gtceu.common.block.BatteryBlock;
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * Variable-size WEN reservoir. Any shell block may be replaced by the controller.
 * Native matching validates a closed 3..15 cuboid, filled cells and exactly one monitor.
 */
public class WENMainStorageMachine extends WorkableElectricMultiblockMachine
        implements IDropSaveMachine {

    private static final Logger LOGGER = LogUtils.getLogger();

    public static final ManagedFieldHolder MANAGED_FIELD_HOLDER =
            new ManagedFieldHolder(
                    WENMainStorageMachine.class,
                    WorkableElectricMultiblockMachine.MANAGED_FIELD_HOLDER);

    public static final int MIN_SIZE = WENStorageStructure.MIN_SIZE;
    public static final int MAX_SIZE = WENStorageStructure.MAX_SIZE;
    public static final int FE_PER_EU = 4;
    public static final double AE_PER_EU = 2.0;
    public static final long EU_PER_CELL = 1_000_000L;

    // Offsets in the native pattern's RIGHT/UP/BACK axes. Old worlds have down/front = 0.
    @Persisted @DescSynced private int lDist = 0, rDist = 0, hDist = 0, bDist = 0;
    @Persisted @DescSynced private int dDist = 0, fDist = 0;
    private WENStorageStructure.Shape matchedShape, cachedShape;
    private BlockPattern cachedPattern;
    @Persisted private int energyCellCount = 0;
    @Persisted private String networkId = "";
    @Persisted private long savedEnergyBackup = 0;
    @Persisted private String savedEnergyBackupExact = "";

    /** 基礎容量は構造成立時に内部の蓄電セルから再計算する。 */
    private java.math.BigInteger maxCapacity = java.math.BigInteger.ZERO;

    /** ストレージアップグレードスロット (1個) — レベル0~36 */
    public static final int MAX_UPGRADE_LEVEL = 36;

    @Persisted private int storageUpgradeLevel = 0;

    /** クロスディメンション結晶（1個のみ） */
    @Persisted private boolean hasCrossDimCrystal = false;

    private final List<BlockPos> inputPortPositions = new ArrayList<>();
    private final List<BlockPos> outputPortPositions = new ArrayList<>();

    private TickableSubscription portTickSub;
    private int portTickOffset = 0;
    @Persisted private int pendingInputFe;
    @Persisted private int prepaidOutputFe;
    @Persisted private double pendingInputAe;
    @Persisted private double prepaidOutputAe;
    private int portLogThrottle = 0;

    public WENMainStorageMachine(IMachineBlockEntity holder) {
        super(holder);
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    // =========================================================================
    //  IDropSaveMachine
    // =========================================================================

    @Override
    public boolean saveBreak() {
        return true;
    }

    @Override
    public void saveToItem(CompoundTag tag) {
        tag.putLong("wen_stored", getStoredEnergy());
        tag.putString("wen_stored_exact", getStoredEnergyBig().toString());
        tag.putString("wen_network", networkId);
        tag.putInt("wen_storage_upgrade", storageUpgradeLevel);
        tag.putBoolean("wen_cross_dimension", hasCrossDimCrystal);
        tag.putInt("wen_pending_input_fe", pendingInputFe);
        tag.putInt("wen_prepaid_output_fe", prepaidOutputFe);
        tag.putDouble("wen_pending_input_ae", pendingInputAe);
        tag.putDouble("wen_prepaid_output_ae", prepaidOutputAe);
    }

    @Override
    public void loadFromItem(CompoundTag tag) {
        if (tag.contains("wen_stored")) savedEnergyBackup = tag.getLong("wen_stored");
        savedEnergyBackupExact = tag.getString("wen_stored_exact");
        if (tag.contains("wen_network")) networkId = tag.getString("wen_network");
        storageUpgradeLevel =
                Math.max(0, Math.min(MAX_UPGRADE_LEVEL, tag.getInt("wen_storage_upgrade")));
        hasCrossDimCrystal = tag.getBoolean("wen_cross_dimension");
        pendingInputFe = Math.max(0, Math.min(FE_PER_EU - 1, tag.getInt("wen_pending_input_fe")));
        prepaidOutputFe = Math.max(0, Math.min(FE_PER_EU - 1, tag.getInt("wen_prepaid_output_fe")));
        pendingInputAe = validAeRemainder(tag.getDouble("wen_pending_input_ae"));
        prepaidOutputAe = validAeRemainder(tag.getDouble("wen_prepaid_output_ae"));
    }

    // =========================================================================
    //  ネットワークID
    // =========================================================================

    public String getNetworkId() {
        return networkId;
    }

    public boolean isNetworkReady() {
        return isFormed() && !getMultiblockState().hasError();
    }

    public boolean setNetworkId(String id) {
        if (!WENNetworkData.isValidId(id)) return false;
        Level level = getLevel();
        if (level == null || level.isClientSide || !isNetworkReady()) return false;
        WENNetworkData data = WENNetworkData.get(level.getServer().overworld());
        String dim = level.dimension().location().toString();
        if (!data.renameNetwork(networkId, id, dim)) return false;
        networkId = id;
        markDirty();
        syncNetworkData();
        applyUpgrades(); // アップグレード済みなら新ネットワークにも反映
        LOGGER.debug("[WEN] Network ID set: {}", id);
        return true;
    }

    private void syncNetworkData() {
        if (networkId.isEmpty()) return;
        Level level = getLevel();
        if (level == null || level.isClientSide) return;
        WENNetworkData data = WENNetworkData.get(level.getServer().overworld());
        boolean restoredRegistration =
                !data.hasNetwork(networkId)
                        && data.registerNetwork(networkId, level.dimension().location().toString());
        data.updateDimension(networkId, level.dimension().location().toString());
        data.updateCapacity(
                networkId,
                isFormed()
                        ? maxCapacity.shiftLeft(
                                Math.max(0, Math.min(storageUpgradeLevel, MAX_UPGRADE_LEVEL)))
                        : java.math.BigInteger.ZERO);
        java.math.BigInteger backup = java.math.BigInteger.valueOf(Math.max(0, savedEnergyBackup));
        if (!savedEnergyBackupExact.isEmpty()) {
            try {
                backup =
                        new java.math.BigInteger(savedEnergyBackupExact)
                                .max(java.math.BigInteger.ZERO);
            } catch (NumberFormatException error) {
                LOGGER.warn("[WEN] Invalid exact backup; using legacy energy backup");
            }
        }
        if (backup.signum() > 0) {
            WENNetworkData.WENEntry entry = data.getNetwork(networkId);
            if (restoredRegistration && entry != null) {
                entry.storedEnergy = backup.min(entry.maxCapacity);
                data.setDirty();
                LOGGER.debug("[WEN] Restored energy from backup: {}EU", entry.storedEnergy);
            }
            savedEnergyBackup = 0;
        }
        savedEnergyBackupExact = "";
        savedEnergyBackup = 0;
    }

    private boolean isWallState(BlockState state) {
        return state.is(ModBlocks.WEN_MAINSTORAGE_CASING.get())
                || state.is(ModBlocks.WEN_MAINSTORAGE_OUTPUT_PORT.get())
                || state.is(ModBlocks.WEN_MAINSTORAGE_INPUT_PORT.get())
                || state.is(ModBlocks.WEN_DATA_MONITOR.get());
    }

    private boolean isWallOrCellState(BlockState state) {
        return isWallState(state)
                || state.is(ModBlocks.WEN_BASIC_ENERGY_CELL.get())
                || state.getBlock() instanceof BatteryBlock battery
                        && battery.getData() != null
                        && battery.getData().getCapacity() > 0;
    }

    private WENStorageStructure.Shape storageShape() {
        var shape = new WENStorageStructure.Shape(lDist, rDist, dDist, hDist, fDist, bDist);
        return shape.valid() ? shape : WENStorageStructure.DEFAULT;
    }

    private BlockPattern patternFor(WENStorageStructure.Shape shape) {
        if (cachedPattern == null || !shape.equals(cachedShape)) {
            cachedPattern = WENStorageStructure.pattern(getDefinition(), shape);
            cachedShape = shape;
        }
        return cachedPattern;
    }

    @Nonnull
    @Override
    public BlockPattern getPattern() {
        // Rendering and layout display use the last formed geometry, without scanning the world.
        return patternFor(storageShape());
    }

    @Override
    public boolean checkPattern() {
        matchedShape = null;
        Level world = getLevel();
        if (world == null) return false;
        var shape =
                WENStorageStructure.detect(
                        getPos(),
                        getFrontFacing(),
                        getUpwardsFacing(),
                        isFlipped(),
                        pos -> world.isOutsideBuildHeight(pos) || world.hasChunkAt(pos),
                        world::getBlockState,
                        this::isWallOrCellState);
        if (shape == null) {
            getMultiblockState().error =
                    new PatternStringError("gtceu.multiblock.invalid_structure");
            return false;
        }
        if (!patternFor(shape).checkPatternAt(getMultiblockState(), false)) return false;
        matchedShape = shape;
        return true;
    }

    private BlockPos structurePosition(int x, int y, int z) {
        return WENStorageStructure.position(
                getPos(), getFrontFacing(), getUpwardsFacing(), isFlipped(), x, y, z);
    }

    // =========================================================================
    //  構造ライフサイクル
    // =========================================================================

    @Override
    public void onStructureFormed() {
        if (matchedShape != null) {
            lDist = matchedShape.left();
            rDist = matchedShape.right();
            dDist = matchedShape.down();
            hDist = matchedShape.up();
            fDist = matchedShape.front();
            bDist = matchedShape.back();
        }
        super.onStructureFormed();
        onChanged();

        // 内部ブロック走査で容量計算 (BigInteger)
        energyCellCount = 0;
        maxCapacity = java.math.BigInteger.ZERO;
        Level level = getLevel();
        if (level != null) {
            for (int db = -fDist + 1; db < bDist; db++) {
                for (int dy = -dDist + 1; dy < hDist; dy++) {
                    for (int dr = -lDist + 1; dr <= rDist - 1; dr++) {
                        BlockPos pos = structurePosition(dr, dy, db);
                        long cellCap = getCellCapacity(level.getBlockState(pos));
                        if (cellCap > 0) {
                            energyCellCount++;
                            maxCapacity = maxCapacity.add(java.math.BigInteger.valueOf(cellCap));
                        }
                    }
                }
            }
        }

        collectPortPositions();

        if (portTickSub == null) portTickSub = subscribeServerTick(this::onPortTick);

        syncNetworkData();
        applyUpgrades();

        LOGGER.debug(
                "[WEN] === FORMED === size={}x{}x{}, cells={}, cap={}EU, stored={}EU, id={}",
                lDist + rDist + 1,
                dDist + hDist + 1,
                fDist + bDist + 1,
                energyCellCount,
                maxCapacity.toString(),
                getStoredEnergy(),
                networkId);
    }

    @Override
    public void onStructureInvalid() {
        super.onStructureInvalid();
        matchedShape = null;
        inputPortPositions.clear();
        outputPortPositions.clear();
        if (portTickSub != null) {
            unsubscribe(portTickSub);
            portTickSub = null;
        }
        if (!networkId.isEmpty() && getLevel() != null && !getLevel().isClientSide) {
            WENNetworkData data = WENNetworkData.get(getLevel().getServer().overworld());
            data.updateCapacity(networkId, 0);
        }
        LOGGER.debug("[WEN] Structure invalid.");
    }

    @Override
    public void onLoad() {
        storageUpgradeLevel = Math.max(0, Math.min(MAX_UPGRADE_LEVEL, storageUpgradeLevel));
        pendingInputFe = Math.max(0, Math.min(FE_PER_EU - 1, pendingInputFe));
        prepaidOutputFe = Math.max(0, Math.min(FE_PER_EU - 1, prepaidOutputFe));
        pendingInputAe = validAeRemainder(pendingInputAe);
        prepaidOutputAe = validAeRemainder(prepaidOutputAe);
        super.onLoad();
    }

    private static double validAeRemainder(double value) {
        return Double.isFinite(value) && value >= 0 && value <= AE_PER_EU ? value : 0;
    }

    @Override
    public void onUnload() {
        super.onUnload();
        if (portTickSub != null) {
            unsubscribe(portTickSub);
            portTickSub = null;
        }
    }

    private static long getCellCapacity(BlockState state) {
        if (state.is(ModBlocks.WEN_BASIC_ENERGY_CELL.get())) return EU_PER_CELL;
        if (state.getBlock() instanceof BatteryBlock batteryBlock) {
            var data = batteryBlock.getData();
            if (data != null) return data.getCapacity();
        }
        return 0;
    }

    // =========================================================================
    //  ポート位置収集
    // =========================================================================

    private void collectPortPositions() {
        inputPortPositions.clear();
        outputPortPositions.clear();
        Level level = getLevel();
        if (level == null) return;
        var shape = storageShape();

        for (int db = -fDist; db <= bDist; db++) {
            for (int dy = -dDist; dy <= hDist; dy++) {
                for (int dr = -lDist; dr <= rDist; dr++) {
                    boolean isShell = shape.shell(dr, dy, db);
                    if (!isShell) continue;

                    BlockPos pos = structurePosition(dr, dy, db);

                    BlockState state = level.getBlockState(pos);
                    if (state.is(ModBlocks.WEN_MAINSTORAGE_INPUT_PORT.get())) {
                        inputPortPositions.add(pos.immutable());
                    } else if (state.is(ModBlocks.WEN_MAINSTORAGE_OUTPUT_PORT.get())) {
                        outputPortPositions.add(pos.immutable());
                    } else if (state.is(ModBlocks.WEN_DATA_MONITOR.get())) {
                        if (level.getBlockEntity(pos)
                                instanceof WENDataMonitorBlockEntity monitor) {
                            monitor.setControllerPos(getPos());
                        }
                    }
                }
            }
        }
        LOGGER.debug(
                "[WEN] Ports: {} in, {} out",
                inputPortPositions.size(),
                outputPortPositions.size());
    }

    // =========================================================================
    //  FEポートtick
    // =========================================================================

    private void onPortTick() {
        if (!isNetworkReady()) return;
        if (++portTickOffset % 5 != 0) return;
        if (networkId.isEmpty()) return;
        Level level = getLevel();
        if (level == null || level.isClientSide) return;

        ServerLevel overworld = level.getServer().overworld();
        WENNetworkData data = WENNetworkData.get(overworld);
        WENNetworkData.WENEntry entry = data.getNetwork(networkId);
        if (entry == null || !entry.isFormed()) return;

        boolean shouldLog = (++portLogThrottle % 20 == 0);

        for (BlockPos portPos : inputPortPositions) {
            if (entry.storedEnergy.compareTo(entry.maxCapacity) >= 0) break;
            pullFeFromOutside(level, data, entry, portPos, shouldLog);
            pullAeFromPort(level, data, entry, portPos, shouldLog);
        }

        for (BlockPos portPos : outputPortPositions) {
            if (entry.storedEnergy.signum() <= 0 && prepaidOutputFe <= 0 && prepaidOutputAe <= 0)
                break;
            pushFeToOutside(level, data, entry, portPos, shouldLog);
            pushAeToPort(level, data, entry, portPos, shouldLog);
        }
    }

    private void pullAeFromPort(
            Level level,
            WENNetworkData data,
            WENNetworkData.WENEntry entry,
            BlockPos portPos,
            boolean log) {
        BlockEntity be = level.getBlockEntity(portPos);
        if (!(be instanceof DIV.gtcsolo.block.wen.WENPortBlockEntity port)) return;
        long spaceEu =
                entry.maxCapacity
                        .subtract(entry.storedEnergy)
                        .min(java.math.BigInteger.valueOf(Long.MAX_VALUE))
                        .longValue();
        if (spaceEu <= 0) return;
        double aeWanted =
                DIV.gtcsolo.util.EnergyMath.receivableInput(
                        spaceEu, pendingInputAe, AE_PER_EU, 1e18);
        double simulated = port.simulateExtractAE(aeWanted);
        if (!Double.isFinite(simulated) || simulated <= 0) return;
        double actualAE = Math.min(aeWanted, simulated);
        double extracted = port.extractAEPower(actualAE);
        if (!Double.isFinite(extracted) || extracted <= 0) return;
        extracted = Math.min(extracted, actualAE);
        var settlement =
                DIV.gtcsolo.util.EnergyMath.settleInput(extracted, pendingInputAe, AE_PER_EU);
        long eu = settlement.eu();
        pendingInputAe = settlement.pending();
        if (extracted > 0) markDirty();
        if (eu >= 1) {
            data.addEnergy(networkId, eu);
            if (log) LOGGER.debug("[WEN Port] Pulled {}AE ({}EU)", extracted, eu);
        }
    }

    private void pushAeToPort(
            Level level,
            WENNetworkData data,
            WENNetworkData.WENEntry entry,
            BlockPos portPos,
            boolean log) {
        BlockEntity be = level.getBlockEntity(portPos);
        if (!(be instanceof DIV.gtcsolo.block.wen.WENPortBlockEntity port)) return;
        long euAvailable =
                entry.storedEnergy
                        .min(java.math.BigInteger.valueOf((long) (Long.MAX_VALUE / AE_PER_EU)))
                        .longValue();
        if (euAvailable <= 0 && prepaidOutputAe <= 0) return;
        double aeToOffer =
                DIV.gtcsolo.util.EnergyMath.payableOutput(euAvailable, prepaidOutputAe, AE_PER_EU);
        double leftover = port.simulateInjectAE(aeToOffer);
        double acceptable = aeToOffer - leftover;
        double actualAE = Math.max(0, Math.min(aeToOffer, acceptable));
        if (actualAE <= 0) return;
        double finalLeftover = port.injectAEPower(actualAE);
        double accepted = Math.max(0, Math.min(actualAE, actualAE - finalLeftover));
        if (accepted > 0) {
            var settlement =
                    DIV.gtcsolo.util.EnergyMath.settleOutput(accepted, prepaidOutputAe, AE_PER_EU);
            data.removeEnergy(networkId, settlement.eu());
            prepaidOutputAe = settlement.prepaid();
            markDirty();
            if (log) LOGGER.debug("[WEN Port] Pushed {}AE ({}EU)", accepted, settlement.eu());
        }
    }

    private void pullFeFromOutside(
            Level level,
            WENNetworkData data,
            WENNetworkData.WENEntry entry,
            BlockPos portPos,
            boolean log) {
        for (Direction dir : Direction.values()) {
            BlockPos neighbor = portPos.relative(dir);
            if (isInsideStructure(neighbor)) continue;
            if (!level.hasChunkAt(neighbor)) continue;
            BlockEntity be = level.getBlockEntity(neighbor);
            if (be == null) continue;
            be.getCapability(ForgeCapabilities.ENERGY, dir.getOpposite())
                    .ifPresent(
                            storage -> {
                                if (!storage.canExtract()) return;
                                long spaceEu =
                                        entry.maxCapacity
                                                .subtract(entry.storedEnergy)
                                                .min(java.math.BigInteger.valueOf(Long.MAX_VALUE))
                                                .longValue();
                                int spaceFe =
                                        (int)
                                                Math.min(
                                                        Math.min(spaceEu, Integer.MAX_VALUE)
                                                                        * FE_PER_EU
                                                                - pendingInputFe,
                                                        Integer.MAX_VALUE);
                                if (spaceFe <= 0) return;
                                int extracted =
                                        Math.max(
                                                0,
                                                Math.min(
                                                        spaceFe,
                                                        storage.extractEnergy(spaceFe, false)));
                                long accumulated = (long) pendingInputFe + extracted;
                                long eu = accumulated / FE_PER_EU;
                                pendingInputFe = (int) (accumulated % FE_PER_EU);
                                if (extracted > 0) markDirty();
                                if (eu > 0) {
                                    data.addEnergy(networkId, eu);
                                    if (log)
                                        LOGGER.debug(
                                                "[WEN Port] Pulled {}FE ({}EU)", extracted, eu);
                                }
                            });
        }
    }

    private void pushFeToOutside(
            Level level,
            WENNetworkData data,
            WENNetworkData.WENEntry entry,
            BlockPos portPos,
            boolean log) {
        for (Direction dir : Direction.values()) {
            BlockPos neighbor = portPos.relative(dir);
            if (isInsideStructure(neighbor)) continue;
            if (!level.hasChunkAt(neighbor)) continue;
            BlockEntity be = level.getBlockEntity(neighbor);
            if (be == null) continue;
            be.getCapability(ForgeCapabilities.ENERGY, dir.getOpposite())
                    .ifPresent(
                            storage -> {
                                if (!storage.canReceive()) return;
                                int feToSend =
                                        (int)
                                                Math.min(
                                                        entry.storedEnergy
                                                                                .min(
                                                                                        java.math
                                                                                                .BigInteger
                                                                                                .valueOf(
                                                                                                        Integer
                                                                                                                .MAX_VALUE))
                                                                                .longValue()
                                                                        * FE_PER_EU
                                                                + prepaidOutputFe,
                                                        Integer.MAX_VALUE);
                                if (feToSend <= 0) return;
                                int accepted =
                                        Math.max(
                                                0,
                                                Math.min(
                                                        feToSend,
                                                        storage.receiveEnergy(feToSend, false)));
                                if (accepted > 0) {
                                    var settlement =
                                            DIV.gtcsolo.util.EnergyMath.settleOutput(
                                                    accepted, prepaidOutputFe, FE_PER_EU);
                                    data.removeEnergy(networkId, settlement.eu());
                                    prepaidOutputFe = (int) settlement.prepaid();
                                    markDirty();
                                    if (log)
                                        LOGGER.debug(
                                                "[WEN Port] Pushed {}FE ({}EU)",
                                                accepted,
                                                settlement.eu());
                                }
                            });
        }
    }

    private boolean isInsideStructure(BlockPos pos) {
        return storageShape()
                .contains(getPos(), getFrontFacing(), getUpwardsFacing(), isFlipped(), pos);
    }

    public boolean containsStructurePosition(BlockPos pos) {
        return isInsideStructure(pos);
    }

    // =========================================================================
    //  アクセサ
    // =========================================================================

    public long getStoredEnergy() {
        if (networkId.isEmpty()) return 0;
        Level level = getLevel();
        if (level == null || level.isClientSide) return 0;
        WENNetworkData data = WENNetworkData.get(level.getServer().overworld());
        WENNetworkData.WENEntry entry = data.getNetwork(networkId);
        return entry != null
                ? entry.storedEnergy.min(java.math.BigInteger.valueOf(Long.MAX_VALUE)).longValue()
                : 0;
    }

    /** BigInteger版 */
    public java.math.BigInteger getStoredEnergyBig() {
        if (networkId.isEmpty()) return java.math.BigInteger.ZERO;
        Level level = getLevel();
        if (level == null || level.isClientSide) return java.math.BigInteger.ZERO;
        WENNetworkData data = WENNetworkData.get(level.getServer().overworld());
        WENNetworkData.WENEntry entry = data.getNetwork(networkId);
        return entry != null ? entry.storedEnergy : java.math.BigInteger.ZERO;
    }

    /** 実効容量（アップグレード適用済み）をSavedDataから取得 */
    public java.math.BigInteger getMaxCapacity() {
        if (networkId.isEmpty()) return maxCapacity;
        Level level = getLevel();
        if (level == null || level.isClientSide) return maxCapacity;
        WENNetworkData data = WENNetworkData.get(level.getServer().overworld());
        WENNetworkData.WENEntry entry = data.getNetwork(networkId);
        return entry != null ? entry.maxCapacity : maxCapacity;
    }

    /** ベース容量（アップグレード前） */
    public java.math.BigInteger getBaseCapacity() {
        return maxCapacity;
    }

    public long getMaxCapacityLong() {
        return getMaxCapacity().min(java.math.BigInteger.valueOf(Long.MAX_VALUE)).longValue();
    }

    public int getEnergyCellCount() {
        return energyCellCount;
    }

    // =========================================================================
    //  アップグレードシステム
    // =========================================================================

    public int getStorageUpgradeLevel() {
        return storageUpgradeLevel;
    }

    public boolean hasCrossDimCrystal() {
        return hasCrossDimCrystal;
    }

    public int getTotalStorageUpgradeLevel() {
        return storageUpgradeLevel;
    }

    public int getNextUpgradeCost() {
        return 2 * (storageUpgradeLevel + 1);
    }

    /** ストレージアップグレード1回実行 */
    public boolean performStorageUpgrade(net.minecraft.server.level.ServerPlayer player) {
        if (storageUpgradeLevel >= MAX_UPGRADE_LEVEL) return false;
        int cost = getNextUpgradeCost();
        if (countFcore(player) < cost) return false;
        consumeFcore(player, cost);
        storageUpgradeLevel++;
        applyUpgrades();
        LOGGER.debug("[WEN] Upgrade: lv={}, cost={}", storageUpgradeLevel, cost);
        return true;
    }

    /** 可能な限り一括レベルアップ */
    public int performBulkUpgrade(net.minecraft.server.level.ServerPlayer player) {
        int upgraded = 0;
        while (storageUpgradeLevel < MAX_UPGRADE_LEVEL) {
            int cost = getNextUpgradeCost();
            if (countFcore(player) < cost) break;
            consumeFcore(player, cost);
            storageUpgradeLevel++;
            upgraded++;
        }
        if (upgraded > 0) {
            applyUpgrades();
            LOGGER.debug("[WEN] Bulk: lv={}, count={}", storageUpgradeLevel, upgraded);
        }
        return upgraded;
    }

    /** レベルダウン（返却用） — fcoreは返さない、レベルのみ下げる */
    public boolean downgradeStorage() {
        if (storageUpgradeLevel <= 0) return false;
        storageUpgradeLevel--;
        applyUpgrades();
        LOGGER.debug("[WEN] Downgrade: lv={}", storageUpgradeLevel);
        return true;
    }

    public static int countFcore(net.minecraft.world.entity.player.Player player) {
        int count = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            var stack = player.getInventory().getItem(i);
            if (stack.is(ModItems.FCORE.get()))
                count = (int) Math.min(Integer.MAX_VALUE, (long) count + stack.getCount());
        }
        return count;
    }

    private static void consumeFcore(net.minecraft.world.entity.player.Player player, int amount) {
        int remaining = amount;
        for (int i = 0; i < player.getInventory().getContainerSize() && remaining > 0; i++) {
            var stack = player.getInventory().getItem(i);
            if (stack.is(ModItems.FCORE.get())) {
                int take = Math.min(remaining, stack.getCount());
                stack.shrink(take);
                remaining -= take;
            }
        }
    }

    /** クロスディメンション結晶の挿入/取り出し */
    public boolean insertCrystal(net.minecraft.server.level.ServerPlayer player) {
        if (hasCrossDimCrystal) return false;
        var held = player.getMainHandItem();
        if (!held.is(ModItems.EVERNIGHT_CRYSTAL.get())) return false;
        held.shrink(1);
        hasCrossDimCrystal = true;
        applyUpgrades();
        return true;
    }

    public boolean removeCrystal(net.minecraft.server.level.ServerPlayer player) {
        if (!hasCrossDimCrystal) return false;
        hasCrossDimCrystal = false;
        var crystal = new net.minecraft.world.item.ItemStack(ModItems.EVERNIGHT_CRYSTAL.get());
        if (!player.getInventory().add(crystal)) player.drop(crystal, false);
        applyUpgrades();
        return true;
    }

    /** 全アップグレード効果をSavedDataに反映 */
    public void applyUpgrades() {
        if (networkId.isEmpty() || getLevel() == null || getLevel().isClientSide) return;
        WENNetworkData data = WENNetworkData.get(getLevel().getServer().overworld());
        WENNetworkData.WENEntry entry = data.getNetwork(networkId);
        if (entry == null) return;

        entry.crossDimensionEnabled = hasCrossDimCrystal;

        int n = Math.max(0, Math.min(getTotalStorageUpgradeLevel(), MAX_UPGRADE_LEVEL));
        java.math.BigInteger effectiveCapacity =
                isFormed() ? maxCapacity.shiftLeft(n) : java.math.BigInteger.ZERO;
        data.updateCapacity(networkId, effectiveCapacity);
        data.setDirty();
        markDirty();

        LOGGER.debug(
                "[WEN] Applied: crossDim={}, n={}, cap={}",
                hasCrossDimCrystal,
                n,
                DIV.gtcsolo.util.EnergyFormat.format(effectiveCapacity));
    }

    /** パケット互換用 */
    public int[] getUpgradeSlotLevels() {
        return new int[] {storageUpgradeLevel};
    }

    // =========================================================================
    //  UI表示
    // =========================================================================

    @Override
    public void addDisplayText(List<Component> textList) {
        super.addDisplayText(textList);
        if (isFormed()) {
            textList.add(
                    Component.translatable(
                            "gtcsolo.machine.wen.id", networkId.isEmpty() ? "---" : networkId));
            textList.add(
                    Component.translatable(
                            "gtcsolo.machine.wen.stored",
                            DIV.gtcsolo.util.EnergyFormat.format(getStoredEnergyBig()),
                            DIV.gtcsolo.util.EnergyFormat.format(getMaxCapacity())));
            textList.add(Component.translatable("gtcsolo.machine.wen.cells", energyCellCount));
            textList.add(
                    Component.translatable(
                            "gtcsolo.machine.wen.ports",
                            inputPortPositions.size(),
                            outputPortPositions.size()));
        }
    }
}
