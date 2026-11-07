package DIV.gtcsolo.integration.ae2;

import DIV.gtcsolo.machine.wen.WENNetworkData;
import DIV.gtcsolo.registry.ModItems;
import appeng.api.config.Actionable;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IInWorldGridNodeHost;
import appeng.api.networking.energy.IEnergyService;
import appeng.api.upgrades.IUpgradeInventory;
import appeng.api.upgrades.IUpgradeableObject;
import appeng.api.upgrades.Upgrades;
import appeng.api.parts.IPart;
import appeng.api.parts.IPartHost;
import appeng.core.definitions.AEBlocks;
import appeng.core.definitions.AEParts;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.slf4j.Logger;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AE2統合: WENカードが挿さっているマシンの所属AE2網に、
 * WEN蓄積からエネルギー注入する (polling方式)
 *
 * 変換レート: 1 EU = 2 AE
 * 探索: ChunkEvent.Load/Unload で IUpgradeableObject 候補BEを追跡、
 *       20tick毎に候補をスキャンしてグリッド単位で給電
 */
public class WENAe2Integration {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static final double AE_PER_EU = 2.0;
    private static final String TOOLTIP_GROUP = "wen_wireless";
    private static final int POLL_INTERVAL = 20; // ticks

    /** 20tickごとの給電上限。実際の注入量はWENの蓄電量とグリッドの受入量に従う。 */
    private static final double MAX_AE_PER_POLL = 1_000_000_000.0;

    /** 次元別候補BE位置 — ChunkEvent.Loadで記録、Unloadで削除 */
    private static final Map<ResourceKey<Level>, Set<BlockPos>> CANDIDATES =
            new ConcurrentHashMap<>();

    /** Unresolved-grid warnings are tracked per dimension and position. */
    private record WarningKey(ResourceKey<Level> dimension, BlockPos pos) {}

    private static final Set<WarningKey> WARNED_NO_GRID = ConcurrentHashMap.newKeySet();

    // =========================================================================
    //  初期化 (commonSetup から呼ぶ)
    // =========================================================================

    public static void registerUpgrades() {
        var card = ModItems.WEN_WIRELESS_ENERGYCARD.get();
        LOGGER.debug("[WEN-AE2] registerUpgrades: card item = {}", card);
        // ブロック型マシン
        Upgrades.add(card, AEBlocks.INTERFACE, 1, TOOLTIP_GROUP);
        Upgrades.add(card, AEBlocks.PATTERN_PROVIDER, 1, TOOLTIP_GROUP);
        Upgrades.add(card, AEBlocks.MOLECULAR_ASSEMBLER, 1, TOOLTIP_GROUP);
        Upgrades.add(card, AEBlocks.INSCRIBER, 1, TOOLTIP_GROUP);
        Upgrades.add(card, AEBlocks.IO_PORT, 1, TOOLTIP_GROUP);
        Upgrades.add(card, AEBlocks.CONDENSER, 1, TOOLTIP_GROUP);
        Upgrades.add(card, AEBlocks.CHARGER, 1, TOOLTIP_GROUP);
        // パート型
        Upgrades.add(card, AEParts.INTERFACE, 1, TOOLTIP_GROUP);
        Upgrades.add(card, AEParts.PATTERN_PROVIDER, 1, TOOLTIP_GROUP);
        Upgrades.add(card, AEParts.IMPORT_BUS, 1, TOOLTIP_GROUP);
        Upgrades.add(card, AEParts.EXPORT_BUS, 1, TOOLTIP_GROUP);
        Upgrades.add(card, AEParts.STORAGE_BUS, 1, TOOLTIP_GROUP);
    }

    // =========================================================================
    //  Chunk イベント — 候補BE追跡
    // =========================================================================

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel sl)) return;
        if (!(event.getChunk() instanceof LevelChunk chunk)) return;
        Set<BlockPos> set =
                CANDIDATES.computeIfAbsent(sl.dimension(), k -> ConcurrentHashMap.newKeySet());
        int added = 0;
        for (var entry : chunk.getBlockEntities().entrySet()) {
            BlockEntity be = entry.getValue();
            if (be instanceof IUpgradeableObject || be instanceof IPartHost) {
                set.add(entry.getKey());
                added++;
            }
        }
        if (added > 0) {
            LOGGER.debug(
                    "[WEN-AE2] ChunkLoad @{} {}: {} candidate BE(s) added (total: {})",
                    sl.dimension().location(),
                    chunk.getPos(),
                    added,
                    set.size());
        }
    }

    @SubscribeEvent
    public static void onChunkUnload(ChunkEvent.Unload event) {
        if (!(event.getLevel() instanceof ServerLevel sl)) return;
        if (!(event.getChunk() instanceof LevelChunk chunk)) return;
        Set<BlockPos> set = CANDIDATES.get(sl.dimension());
        for (var pos : chunk.getBlockEntities().keySet()) {
            if (set != null) set.remove(pos);
            WARNED_NO_GRID.remove(new WarningKey(sl.dimension(), pos));
        }
    }

    @SubscribeEvent
    public static void onBlockPlaced(
            net.minecraftforge.event.level.BlockEvent.EntityPlaceEvent event) {
        if (event.getLevel() instanceof ServerLevel level
                && event.getPlacedBlock().hasBlockEntity()) {
            CANDIDATES
                    .computeIfAbsent(level.dimension(), k -> ConcurrentHashMap.newKeySet())
                    .add(event.getPos().immutable());
        }
    }

    @SubscribeEvent
    public static void onServerStopped(net.minecraftforge.event.server.ServerStoppedEvent event) {
        CANDIDATES.clear();
        WARNED_NO_GRID.clear();
    }

    private record GridSupply(String networkId, ItemStack card, BlockEntity holder) {}

    private static final String PREPAID_AE = "gtcsolo_wen_prepaid_ae";

    // =========================================================================
    //  サーバーtick — 20tick毎に全次元の候補BEをスキャンして給電
    // =========================================================================

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        MinecraftServer server = event.getServer();
        if (server == null) return;
        if (server.getTickCount() % POLL_INTERVAL != 0) return;

        for (ServerLevel level : server.getAllLevels()) {
            processLevel(level, server);
        }
    }

    private static void processLevel(ServerLevel level, MinecraftServer server) {
        Set<BlockPos> candidates = CANDIDATES.get(level.dimension());
        int candidateCount = candidates == null ? 0 : candidates.size();
        boolean verbose = server.getTickCount() % (POLL_INTERVAL * 10) == 0;
        if (candidates == null || candidates.isEmpty()) {
            return;
        }

        WENNetworkData data = WENNetworkData.find(server.overworld());
        if (data == null) return;
        ScanStats stats = new ScanStats();

        // grid → networkId (first-wins で重複整理)
        Map<IGrid, GridSupply> gridToNetwork = new HashMap<>();

        for (BlockPos pos : candidates) {
            if (!level.hasChunkAt(pos)) {
                candidates.remove(pos);
                WARNED_NO_GRID.remove(new WarningKey(level.dimension(), pos));
                continue;
            }
            BlockEntity be = level.getBlockEntity(pos);
            if (!(be instanceof IUpgradeableObject) && !(be instanceof IPartHost)) {
                candidates.remove(pos);
                WARNED_NO_GRID.remove(new WarningKey(level.dimension(), pos));
                continue;
            }
            stats.scanned++;

            // (1) BE 自身がIUpgradeableObject
            if (be instanceof IUpgradeableObject upgradeable) {
                scanHolder(
                        upgradeable,
                        () -> resolveGrid(be),
                        pos,
                        be.getClass().getSimpleName(),
                        be,
                        gridToNetwork,
                        stats,
                        data);
            }

            // (2) BE が IPartHost なら各パート (6方向 + 中央) もチェック
            if (be instanceof IPartHost partHost) {
                for (Direction d : Direction.values()) {
                    scanPart(partHost.getPart(d), be, pos, gridToNetwork, stats, data);
                }
                scanPart(partHost.getPart((Direction) null), be, pos, gridToNetwork, stats, data);
            }
        }

        // 統計: verbose (10秒毎)、それ以外の間もカードがあればDEBUG
        if (verbose) {
            LOGGER.debug(
                    "[WEN-AE2] {}: candidates={}, scanned={}, withCard={}, bound={}, gridResolved={}, uniqueGrids={}",
                    level.dimension().location(),
                    candidateCount,
                    stats.scanned,
                    stats.withCard,
                    stats.boundCards,
                    stats.gridResolved,
                    gridToNetwork.size());
        } else if (stats.withCard > 0) {
            LOGGER.debug(
                    "[WEN-AE2] {}: bound={}, gridResolved={}",
                    level.dimension().location(),
                    stats.boundCards,
                    stats.gridResolved);
        }

        if (gridToNetwork.isEmpty()) return;

        for (var entry : gridToNetwork.entrySet()) {
            supplyGrid(entry.getKey(), entry.getValue(), data, level, verbose);
        }
    }

    private static final class ScanStats {
        int scanned;
        int withCard;
        int boundCards;
        int gridResolved;
    }

    /** パート1つ分: 自身の grid node を優先し、無ければホストBE経由で解決する。 */
    private static void scanPart(
            IPart part,
            BlockEntity hostBe,
            BlockPos pos,
            Map<IGrid, GridSupply> gridToNetwork,
            ScanStats stats,
            WENNetworkData data) {
        if (!(part instanceof IUpgradeableObject upPart)) return;
        scanHolder(
                upPart,
                () -> resolvePartGrid(part, hostBe),
                pos,
                part.getClass().getSimpleName(),
                hostBe,
                gridToNetwork,
                stats,
                data);
    }

    /**
     * カード保持オブジェクト (BE またはパート) 1つ分の共通処理:
     * カード検出 → networkId 取得 → grid 解決 → gridToNetwork へ記録。
     * grid 未解決は pos 毎に1回だけ WARN (解決したら抑制を解除)。
     */
    private static void scanHolder(
            IUpgradeableObject holder,
            java.util.function.Supplier<IGrid> gridResolver,
            BlockPos pos,
            String what,
            BlockEntity source,
            Map<IGrid, GridSupply> gridToNetwork,
            ScanStats stats,
            WENNetworkData data) {
        ItemStack card = findCard(holder.getUpgrades());
        if (card.isEmpty()) return;
        stats.withCard++;
        String networkId = WENEnergyCardItem.getBoundNetworkId(card);
        LOGGER.trace("[WEN-AE2] Card in {}@{}, networkId='{}'", what, pos, networkId);
        if (networkId.isEmpty()) return;
        stats.boundCards++;
        if (source.getLevel() == null) return;
        var warningKey = new WarningKey(source.getLevel().dimension(), pos);
        if (data.accessibleNetwork(networkId, source.getLevel().dimension().location().toString())
                == null) {
            WARNED_NO_GRID.remove(warningKey);
            return;
        }
        IGrid grid = gridResolver.get();
        if (grid != null) {
            stats.gridResolved++;
            gridToNetwork.putIfAbsent(grid, new GridSupply(networkId, card, source));
            WARNED_NO_GRID.remove(warningKey);
        } else if (WARNED_NO_GRID.add(warningKey)) {
            LOGGER.warn(
                    "[WEN-AE2] {}@{} has bound card but NO grid — not connected to ME network?",
                    what,
                    pos);
        }
    }

    private static ItemStack findCard(IUpgradeInventory inv) {
        for (int i = 0; i < inv.size(); i++) {
            ItemStack stack = inv.getStackInSlot(i);
            if (stack.getItem() == ModItems.WEN_WIRELESS_ENERGYCARD.get()) return stack;
        }
        return ItemStack.EMPTY;
    }

    private static IGrid resolveGrid(BlockEntity be) {
        if (be instanceof IInWorldGridNodeHost host) {
            IGridNode node = host.getGridNode(null);
            if (node != null) return node.getGrid();
        }
        return null;
    }

    /**
     * パートの grid 解決。cable bus の {@code getGridNode(null)} は構成によって null を
     * 返すため、まずパート自身の {@link IPart#getGridNode()} を使う (バグ修正 2026-07-02)。
     */
    private static IGrid resolvePartGrid(IPart part, BlockEntity hostBe) {
        IGridNode node = part.getGridNode();
        if (node != null) return node.getGrid();
        return resolveGrid(hostBe);
    }

    // =========================================================================
    //  実際の給電: AE2 grid.energyService.injectPower で注入、WENから消費
    // =========================================================================

    private static void supplyGrid(
            IGrid grid,
            GridSupply supply,
            WENNetworkData data,
            ServerLevel level,
            boolean verbose) {
        String networkId = supply.networkId();
        WENNetworkData.WENEntry entry = data.getNetwork(networkId);
        if (entry == null) {
            // networkId不整合 (モニター破壊後にカードだけ残ってる等) — 稀なので毎回WARN
            LOGGER.debug(
                    "[WEN-AE2] Network '{}' not found (card bound to deleted network?)", networkId);
            return;
        }
        if (!entry.isFormed()
                || (!entry.crossDimensionEnabled
                        && !level.dimension().location().toString().equals(entry.dimension)))
            return;
        double prepaid = supply.card().hasTag() ? supply.card().getTag().getDouble(PREPAID_AE) : 0;
        if (!Double.isFinite(prepaid) || prepaid < 0 || prepaid > AE_PER_EU) prepaid = 0;
        if (entry.storedEnergy.signum() <= 0 && prepaid <= 0) return;

        IEnergyService es = grid.getEnergyService();
        double simLeftover = es.injectPower(MAX_AE_PER_POLL, Actionable.SIMULATE);
        double aeAcceptable = MAX_AE_PER_POLL - simLeftover;
        if (!Double.isFinite(aeAcceptable) || aeAcceptable <= 0) return;

        long euRequested = (long) Math.ceil(aeAcceptable / AE_PER_EU);
        long euAvailable = entry.storedEnergy.min(BigInteger.valueOf(euRequested)).longValue();
        double aeToSend =
                Math.min(
                        aeAcceptable,
                        DIV.gtcsolo.util.EnergyMath.payableOutput(euAvailable, prepaid, AE_PER_EU));
        if (aeToSend <= 0) return;
        double finalLeftover = es.injectPower(aeToSend, Actionable.MODULATE);
        double accepted = Math.max(0, Math.min(aeToSend, aeToSend - finalLeftover));
        if (accepted > 0) {
            var settlement = DIV.gtcsolo.util.EnergyMath.settleOutput(accepted, prepaid, AE_PER_EU);
            data.removeEnergy(networkId, settlement.eu());
            supply.card().getOrCreateTag().putDouble(PREPAID_AE, settlement.prepaid());
            supply.holder().setChanged();
            if (verbose)
                LOGGER.debug(
                        "[WEN-AE2] Supplied '{}': {} AE ({} EU), WEN remaining={}",
                        networkId,
                        accepted,
                        settlement.eu(),
                        entry.storedEnergy);
        }
    }
}
