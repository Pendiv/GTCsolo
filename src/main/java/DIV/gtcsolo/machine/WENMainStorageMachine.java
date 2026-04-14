package DIV.gtcsolo.machine;

import DIV.gtcsolo.block.wen.WENDataMonitorBlockEntity;
import DIV.gtcsolo.machine.wen.WENNetworkData;
import DIV.gtcsolo.registry.ModBlocks;
import DIV.gtcsolo.registry.ModItems;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.TickableSubscription;
import com.gregtechceu.gtceu.api.machine.feature.IDropSaveMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.pattern.BlockPattern;
import com.gregtechceu.gtceu.api.pattern.FactoryBlockPattern;
import com.gregtechceu.gtceu.api.pattern.TraceabilityPredicate;
import com.gregtechceu.gtceu.common.block.BatteryBlock;
import com.gregtechceu.gtceu.common.data.GTBlocks;
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

import static com.gregtechceu.gtceu.api.pattern.Predicates.*;
import static com.gregtechceu.gtceu.api.pattern.util.RelativeDirection.*;

/**
 * WEN Main Storage — 可変サイズマルチブロック
 *
 * コントローラーは壁面の最下段に配置。
 * 構造: 外装(casing/port/monitor)で覆い、内部を蓄電セルで充填。
 * サイズ: 3×3×3 ～ 15×15×15 (長方形OK)
 * data_monitor: ちょうど1基必要
 *
 * 座標系 (コントローラー視点):
 *   front = 外側、back = 構造内部
 *   left/right = 横
 *   up = 上 (コントローラーは最下段なので下方向はない)
 *
 * パターン方向: RIGHT, UP, BACK
 *   aisle[0] = コントローラー面 (front)
 *   aisle[1..N-2] = 中間層 (外周W + 内部S)
 *   aisle[N-1] = 奥面 (back)
 *   各aisle = [横] × [縦]
 *   C位置 = (lDist, 0) = 最下段
 */
public class WENMainStorageMachine extends WorkableElectricMultiblockMachine implements IDropSaveMachine {

    private static final Logger LOGGER = LogUtils.getLogger();

    public static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            WENMainStorageMachine.class, WorkableElectricMultiblockMachine.MANAGED_FIELD_HOLDER);

    public static final int MIN_SIZE = 3;
    public static final int MAX_SIZE = 15;
    public static final int FE_PER_EU = 4;
    public static final long EU_PER_CELL = 1_000_000L;

    // lDist/rDist = コントローラーから左右の壁まで
    // hDist = コントローラーから天井まで (コントローラーは最下段)
    // bDist = コントローラーから奥の壁まで
    @Persisted private int lDist = 0, rDist = 0, hDist = 0, bDist = 0;
    @Persisted private int energyCellCount = 0;
    @Persisted private String networkId = "";
    @Persisted private long savedEnergyBackup = 0;
    /** 容量はBigInteger（@Persistedではなく手動NBT） */
    private java.math.BigInteger maxCapacity = java.math.BigInteger.ZERO;

    /** ストレージアップグレードスロット (1個) — レベル0~36 */
    public static final int MAX_UPGRADE_LEVEL = 36;
    @Persisted
    private int storageUpgradeLevel = 0;
    /** クロスディメンション結晶（1個のみ） */
    @Persisted
    private boolean hasCrossDimCrystal = false;

    private final List<BlockPos> inputPortPositions = new ArrayList<>();
    private final List<BlockPos> outputPortPositions = new ArrayList<>();

    private TickableSubscription portTickSub;
    private int portTickOffset = 0;
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

    @Override public boolean saveBreak() { return true; }

    @Override
    public void saveToItem(CompoundTag tag) {
        tag.putLong("wen_stored", getStoredEnergy());
        tag.putString("wen_network", networkId);
    }

    @Override
    public void loadFromItem(CompoundTag tag) {
        if (tag.contains("wen_stored")) savedEnergyBackup = tag.getLong("wen_stored");
        if (tag.contains("wen_network")) networkId = tag.getString("wen_network");
    }

    // =========================================================================
    //  ネットワークID
    // =========================================================================

    public String getNetworkId() { return networkId; }

    public boolean setNetworkId(String id) {
        if (!WENNetworkData.isValidId(id)) return false;
        Level level = getLevel();
        if (level == null || level.isClientSide) return false;
        WENNetworkData data = WENNetworkData.get(level.getServer().overworld());
        if (!networkId.isEmpty() && data.hasNetwork(networkId)) data.removeNetwork(networkId);
        String dim = level.dimension().location().toString();
        if (!data.registerNetwork(id, dim)) return false;
        networkId = id;
        syncNetworkData();
        LOGGER.info("[WEN] Network ID set: {}", id);
        return true;
    }

    private void syncNetworkData() {
        if (networkId.isEmpty()) return;
        Level level = getLevel();
        if (level == null || level.isClientSide) return;
        WENNetworkData data = WENNetworkData.get(level.getServer().overworld());
        data.updateCapacity(networkId, maxCapacity);
        if (savedEnergyBackup > 0) {
            WENNetworkData.WENEntry entry = data.getNetwork(networkId);
            if (entry != null && entry.storedEnergy.signum() == 0) {
                entry.storedEnergy = java.math.BigInteger.valueOf(savedEnergyBackup).min(maxCapacity);
                LOGGER.info("[WEN] Restored energy from backup: {}EU", entry.storedEnergy);
            }
            savedEnergyBackup = 0;
        }
    }

    // =========================================================================
    //  構造サイズ検出
    //  コントローラー = 壁面の最下段
    //  → dDist は常に 0
    //  → 上方向、左右、奥行きを走査
    // =========================================================================

    public void updateStructureDimensions() {
        Level world = getLevel();
        if (world == null) return;

        Direction front = getFrontFacing();
        Direction back = front.getOpposite();
        Direction left = front.getCounterClockWise();
        Direction right = left.getOpposite();

        BlockPos.MutableBlockPos lPos = getPos().mutable();
        BlockPos.MutableBlockPos rPos = getPos().mutable();
        BlockPos.MutableBlockPos uPos = getPos().mutable();
        BlockPos.MutableBlockPos bPos = getPos().mutable();

        int lDist = 0, rDist = 0, hDist = 0, bDist = 0;

        // 左右走査: コントローラーは壁面なので隣も壁。
        // 壁ブロックが途切れるまで走査し、最後の壁の位置を端とする。
        for (int i = 1; i < MAX_SIZE; i++) {
            if (isWallBlockAt(world, getPos(), left, i)) lDist = i;
            else break;
        }
        for (int i = 1; i < MAX_SIZE; i++) {
            if (isWallBlockAt(world, getPos(), right, i)) rDist = i;
            else break;
        }

        // 上方向走査: コントローラーは最下段=床面の壁。
        // 上に進んで壁→内部(or壁)→壁と推移する。
        // 壁または蓄電セルが続く限り走査し、何もない位置で止める。
        // 最後に壁ブロックだった位置が天井。
        for (int i = 1; i < MAX_SIZE; i++) {
            BlockPos checkPos = getPos().relative(Direction.UP, i);
            BlockState st = world.getBlockState(checkPos);
            if (isWallOrCellState(st)) {
                hDist = i;
            } else {
                break; // 構造外
            }
        }

        // 奥行き走査: front→backに向かって同様。
        for (int i = 1; i < MAX_SIZE; i++) {
            BlockPos checkPos = getPos().relative(back, i);
            BlockState st = world.getBlockState(checkPos);
            if (isWallOrCellState(st)) {
                bDist = i;
            } else {
                break;
            }
        }

        int totalW = lDist + rDist + 1;
        int totalH = hDist + 1;      // コントローラー行 + 上
        int totalD = bDist + 1;      // コントローラー面 + 奥行き

        LOGGER.info("[WEN] Scan: L={} R={} H={} B={} → {}x{}x{} (facing={})",
                lDist, rDist, hDist, bDist, totalW, totalH, totalD, front);

        if (totalW < MIN_SIZE || totalH < MIN_SIZE || totalD < MIN_SIZE) {
            LOGGER.info("[WEN] Too small: {}x{}x{} (min {})", totalW, totalH, totalD, MIN_SIZE);
            this.isFormed = false;
            return;
        }
        if (totalW > MAX_SIZE || totalH > MAX_SIZE || totalD > MAX_SIZE) {
            LOGGER.info("[WEN] Too large: {}x{}x{} (max {})", totalW, totalH, totalD, MAX_SIZE);
            this.isFormed = false;
            return;
        }
        if (bDist < 2) {
            LOGGER.info("[WEN] Depth too shallow: bDist={}", bDist);
            this.isFormed = false;
            return;
        }

        this.lDist = lDist;
        this.rDist = rDist;
        this.hDist = hDist;
        this.bDist = bDist;
    }

    /** 指定方向にi個進んだ位置が壁ブロックか */
    private boolean isWallBlockAt(Level world, BlockPos origin, Direction dir, int dist) {
        BlockState state = world.getBlockState(origin.relative(dir, dist));
        return isWallState(state);
    }

    private boolean isWallState(BlockState state) {
        return state.is(ModBlocks.WEN_MAINSTORAGE_CASING.get())
            || state.is(ModBlocks.WEN_MAINSTORAGE_OUTPUT_PORT.get())
            || state.is(ModBlocks.WEN_MAINSTORAGE_INPUT_PORT.get())
            || state.is(ModBlocks.WEN_DATA_MONITOR.get());
    }

    private boolean isWallOrCellState(BlockState state) {
        return isWallState(state) || isCellState(state);
    }

    private boolean isCellState(BlockState state) {
        if (state.is(ModBlocks.WEN_BASIC_ENERGY_CELL.get())) return true;
        return state.getBlock() instanceof BatteryBlock;
    }

    // =========================================================================
    //  動的パターン生成
    //
    //  FactoryBlockPattern.start(RIGHT, UP, BACK):
    //    各aisleは奥行き方向の1スライス
    //    aisle内 = [横] x [縦] の文字列配列
    //
    //  C位置 = aisle[0](front面) の (lDist, 0) = 左からlDist番目、最下行
    // =========================================================================

    @Nonnull
    @Override
    public BlockPattern getPattern() {
        if (getLevel() != null) updateStructureDimensions();

        // JEIガード
        if (lDist < 1) lDist = 1;
        if (rDist < 1) rDist = 1;
        if (hDist < 2) hDist = 2;
        if (bDist < 2) bDist = 2;

        int width = lDist + rDist + 1;
        int height = hDist + 1;
        int middleLayers = bDist - 1;

        // front面: 全W、最下行のlDist位置にC
        String[] frontFace = new String[height];
        for (int y = 0; y < height; y++) {
            StringBuilder row = new StringBuilder(width);
            for (int x = 0; x < width; x++) {
                if (y == 0 && x == lDist) row.append('C');
                else row.append('W');
            }
            frontFace[y] = row.toString();
        }

        // 中間層: 外周W、内部S
        String[] middleSlice = new String[height];
        for (int y = 0; y < height; y++) {
            StringBuilder row = new StringBuilder(width);
            for (int x = 0; x < width; x++) {
                boolean edge = (x == 0 || x == width - 1 || y == 0 || y == height - 1);
                row.append(edge ? 'W' : 'S');
            }
            middleSlice[y] = row.toString();
        }

        // 奥面: 全W
        String[] backFace = new String[height];
        for (int y = 0; y < height; y++) {
            StringBuilder row = new StringBuilder(width);
            for (int x = 0; x < width; x++) row.append('W');
            backFace[y] = row.toString();
        }

        TraceabilityPredicate wallPredicate = blocks(ModBlocks.WEN_MAINSTORAGE_CASING.get())
                .or(blocks(ModBlocks.WEN_MAINSTORAGE_OUTPUT_PORT.get()))
                .or(blocks(ModBlocks.WEN_MAINSTORAGE_INPUT_PORT.get()))
                .or(blocks(ModBlocks.WEN_DATA_MONITOR.get()).setMinGlobalLimited(1).setMaxGlobalLimited(1));

        TraceabilityPredicate cellPredicate = blocks(
                ModBlocks.WEN_BASIC_ENERGY_CELL.get(),
                GTBlocks.BATTERY_LAPOTRONIC_EV.get(),
                GTBlocks.BATTERY_LAPOTRONIC_IV.get(),
                GTBlocks.BATTERY_LAPOTRONIC_LuV.get(),
                GTBlocks.BATTERY_LAPOTRONIC_ZPM.get(),
                GTBlocks.BATTERY_LAPOTRONIC_UV.get(),
                GTBlocks.BATTERY_ULTIMATE_UHV.get());

        LOGGER.info("[WEN] Pattern: {}x{}x{}, middleLayers={}, C at ({},0)",
                width, height, bDist + 1, middleLayers, lDist);

        return FactoryBlockPattern.start(RIGHT, UP, BACK)
                .aisle(frontFace)
                .aisle(middleSlice).setRepeatable(middleLayers)
                .aisle(backFace)
                .where('C', controller(blocks(this.getDefinition().get())))
                .where('W', wallPredicate)
                .where('S', cellPredicate)
                .build();
    }

    // =========================================================================
    //  構造ライフサイクル
    // =========================================================================

    @Override
    public void onStructureFormed() {
        super.onStructureFormed();

        // 内部ブロック走査で容量計算 (BigInteger)
        energyCellCount = 0;
        maxCapacity = java.math.BigInteger.ZERO;
        Level level = getLevel();
        if (level != null) {
            Direction back = getFrontFacing().getOpposite();
            Direction right = getFrontFacing().getCounterClockWise().getOpposite();

            for (int db = 1; db < bDist; db++) {
                for (int dy = 1; dy < hDist; dy++) {
                    for (int dr = -lDist + 1; dr <= rDist - 1; dr++) {
                        BlockPos pos = getPos()
                                .relative(back, db)
                                .relative(right, dr)
                                .relative(Direction.UP, dy);
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

        LOGGER.info("[WEN] === FORMED === size={}x{}x{}, cells={}, cap={}EU, stored={}EU, id={}",
                lDist + rDist + 1, hDist + 1, bDist + 1,
                energyCellCount, maxCapacity.toString(), getStoredEnergy(), networkId);
    }

    @Override
    public void onStructureInvalid() {
        super.onStructureInvalid();
        inputPortPositions.clear();
        outputPortPositions.clear();
        if (portTickSub != null) { unsubscribe(portTickSub); portTickSub = null; }
        if (!networkId.isEmpty() && getLevel() != null && !getLevel().isClientSide) {
            WENNetworkData data = WENNetworkData.get(getLevel().getServer().overworld());
            data.updateCapacity(networkId, 0);
        }
        LOGGER.info("[WEN] Structure invalid.");
    }

    @Override
    public void onUnload() {
        super.onUnload();
        if (portTickSub != null) { unsubscribe(portTickSub); portTickSub = null; }
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

        Direction back = getFrontFacing().getOpposite();
        Direction right = getFrontFacing().getCounterClockWise().getOpposite();

        for (int db = 0; db <= bDist; db++) {
            for (int dy = 0; dy <= hDist; dy++) {
                for (int dr = -lDist; dr <= rDist; dr++) {
                    boolean isShell = (db == 0 || db == bDist ||
                            dr == -lDist || dr == rDist ||
                            dy == 0 || dy == hDist);
                    if (!isShell) continue;

                    BlockPos pos = getPos()
                            .relative(back, db)
                            .relative(right, dr)
                            .relative(Direction.UP, dy);

                    BlockState state = level.getBlockState(pos);
                    if (state.is(ModBlocks.WEN_MAINSTORAGE_INPUT_PORT.get())) {
                        inputPortPositions.add(pos.immutable());
                    } else if (state.is(ModBlocks.WEN_MAINSTORAGE_OUTPUT_PORT.get())) {
                        outputPortPositions.add(pos.immutable());
                    } else if (state.is(ModBlocks.WEN_DATA_MONITOR.get())) {
                        if (level.getBlockEntity(pos) instanceof WENDataMonitorBlockEntity monitor) {
                            monitor.setControllerPos(getPos());
                        }
                    }
                }
            }
        }
        LOGGER.info("[WEN] Ports: {} in, {} out", inputPortPositions.size(), outputPortPositions.size());
    }

    // =========================================================================
    //  FEポートtick
    // =========================================================================

    private void onPortTick() {
        if (!isFormed()) return;
        if (++portTickOffset % 5 != 0) return;
        if (networkId.isEmpty()) return;
        Level level = getLevel();
        if (level == null || level.isClientSide) return;

        ServerLevel overworld = level.getServer().overworld();
        WENNetworkData data = WENNetworkData.get(overworld);
        WENNetworkData.WENEntry entry = data.getNetwork(networkId);
        if (entry == null) return;

        boolean shouldLog = (++portLogThrottle % 20 == 0);

        for (BlockPos portPos : inputPortPositions) {
            if (entry.storedEnergy.compareTo(entry.maxCapacity) >= 0) break;
            pullFeFromOutside(level, data, entry, portPos, shouldLog);
        }

        for (BlockPos portPos : outputPortPositions) {
            if (entry.storedEnergy.signum() <= 0) break;
            pushFeToOutside(level, data, entry, portPos, shouldLog);
        }
    }

    private void pullFeFromOutside(Level level, WENNetworkData data,
                                    WENNetworkData.WENEntry entry, BlockPos portPos, boolean log) {
        for (Direction dir : Direction.values()) {
            BlockPos neighbor = portPos.relative(dir);
            if (isInsideStructure(neighbor)) continue;
            BlockEntity be = level.getBlockEntity(neighbor);
            if (be == null) continue;
            be.getCapability(ForgeCapabilities.ENERGY, dir.getOpposite()).ifPresent(storage -> {
                if (!storage.canExtract()) return;
                long spaceEu = entry.maxCapacity.subtract(entry.storedEnergy)
                        .min(java.math.BigInteger.valueOf(Long.MAX_VALUE)).longValue();
                int spaceFe = (int) Math.min(spaceEu * FE_PER_EU, Integer.MAX_VALUE);
                int extracted = storage.extractEnergy(spaceFe, false);
                if (extracted >= FE_PER_EU) {
                    long eu = extracted / FE_PER_EU;
                    data.addEnergy(networkId, eu);
                    if (log) LOGGER.info("[WEN Port] Pulled {}FE ({}EU)", extracted, eu);
                }
            });
        }
    }

    private void pushFeToOutside(Level level, WENNetworkData data,
                                  WENNetworkData.WENEntry entry, BlockPos portPos, boolean log) {
        for (Direction dir : Direction.values()) {
            BlockPos neighbor = portPos.relative(dir);
            if (isInsideStructure(neighbor)) continue;
            BlockEntity be = level.getBlockEntity(neighbor);
            if (be == null) continue;
            be.getCapability(ForgeCapabilities.ENERGY, dir.getOpposite()).ifPresent(storage -> {
                if (!storage.canReceive()) return;
                int feToSend = (int) Math.min(
                        entry.storedEnergy.min(java.math.BigInteger.valueOf(Long.MAX_VALUE / FE_PER_EU)).longValue() * FE_PER_EU,
                        Integer.MAX_VALUE);
                int accepted = storage.receiveEnergy(feToSend, false);
                if (accepted >= FE_PER_EU) {
                    long eu = accepted / FE_PER_EU;
                    data.removeEnergy(networkId, eu);
                    if (log) LOGGER.info("[WEN Port] Pushed {}FE ({}EU)", accepted, eu);
                }
            });
        }
    }

    private boolean isInsideStructure(BlockPos pos) {
        BlockPos origin = getPos();
        Direction back = getFrontFacing().getOpposite();
        Direction right = getFrontFacing().getCounterClockWise().getOpposite();
        int db = dotDir(pos, origin, back);
        int dr = dotDir(pos, origin, right);
        int dy = pos.getY() - origin.getY();
        return db >= 0 && db <= bDist && dr >= -lDist && dr <= rDist && dy >= 0 && dy <= hDist;
    }

    private int dotDir(BlockPos pos, BlockPos origin, Direction dir) {
        return (pos.getX() - origin.getX()) * dir.getStepX()
             + (pos.getZ() - origin.getZ()) * dir.getStepZ();
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
        return entry != null ? entry.storedEnergy.min(java.math.BigInteger.valueOf(Long.MAX_VALUE)).longValue() : 0;
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
    public java.math.BigInteger getBaseCapacity() { return maxCapacity; }
    public long getMaxCapacityLong() {
        return getMaxCapacity().min(java.math.BigInteger.valueOf(Long.MAX_VALUE)).longValue();
    }
    public int getEnergyCellCount() { return energyCellCount; }

    // =========================================================================
    //  アップグレードシステム
    // =========================================================================

    public int getStorageUpgradeLevel() { return storageUpgradeLevel; }
    public boolean hasCrossDimCrystal() { return hasCrossDimCrystal; }

    public int getTotalStorageUpgradeLevel() { return storageUpgradeLevel; }

    public int getNextUpgradeCost() { return 2 * (storageUpgradeLevel + 1); }

    /** ストレージアップグレード1回実行 */
    public boolean performStorageUpgrade(net.minecraft.server.level.ServerPlayer player) {
        if (storageUpgradeLevel >= MAX_UPGRADE_LEVEL) return false;
        int cost = getNextUpgradeCost();
        if (countFcore(player) < cost) return false;
        consumeFcore(player, cost);
        storageUpgradeLevel++;
        applyUpgrades();
        LOGGER.info("[WEN] Upgrade: lv={}, cost={}", storageUpgradeLevel, cost);
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
            LOGGER.info("[WEN] Bulk: lv={}, count={}", storageUpgradeLevel, upgraded);
        }
        return upgraded;
    }

    /** レベルダウン（返却用） — fcoreは返さない、レベルのみ下げる */
    public boolean downgradeStorage() {
        if (storageUpgradeLevel <= 0) return false;
        storageUpgradeLevel--;
        applyUpgrades();
        LOGGER.info("[WEN] Downgrade: lv={}", storageUpgradeLevel);
        return true;
    }

    public static int countFcore(net.minecraft.world.entity.player.Player player) {
        int count = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            var stack = player.getInventory().getItem(i);
            if (stack.is(ModItems.FCORE.get())) count += stack.getCount();
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

        int n = getTotalStorageUpgradeLevel();
        java.math.BigInteger effectiveCapacity = maxCapacity.shiftLeft(n);
        data.updateCapacity(networkId, effectiveCapacity);
        data.setDirty();

        LOGGER.info("[WEN] Applied: crossDim={}, n={}, cap={}",
                hasCrossDimCrystal, n, DIV.gtcsolo.util.EnergyFormat.format(effectiveCapacity));
    }

    /** パケット互換用 */
    public int[] getUpgradeSlotLevels() { return new int[]{ storageUpgradeLevel }; }

    // =========================================================================
    //  UI表示
    // =========================================================================

    @Override
    public void addDisplayText(List<Component> textList) {
        super.addDisplayText(textList);
        if (isFormed()) {
            textList.add(Component.translatable("gtcsolo.machine.wen.id",
                    networkId.isEmpty() ? "---" : networkId));
            textList.add(Component.translatable("gtcsolo.machine.wen.stored",
                    DIV.gtcsolo.util.EnergyFormat.format(getStoredEnergyBig()),
                    DIV.gtcsolo.util.EnergyFormat.format(maxCapacity)));
            textList.add(Component.translatable("gtcsolo.machine.wen.cells", energyCellCount));
            textList.add(Component.translatable("gtcsolo.machine.wen.ports",
                    inputPortPositions.size(), outputPortPositions.size()));
        }
    }
}
