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
    /** 給電上限 (AE/tick, 1回の注入量。WEN蓄積とグリッド受入量の min が実際の量) */
    private static final double MAX_AE_PER_POLL = 1_000_000_000.0;

    /** 次元別候補BE位置 — ChunkEvent.Loadで記録、Unloadで削除 */
    private static final Map<ResourceKey<Level>, Set<BlockPos>> CANDIDATES = new ConcurrentHashMap<>();

    /** 詳細ログ間引き用カウンタ */
    private static int logTickCounter = 0;
    /** NO grid 警告の重複抑制 (pos毎に1回のみ警告) */
    private static final Set<BlockPos> WARNED_NO_GRID = ConcurrentHashMap.newKeySet();

    // =========================================================================
    //  初期化 (commonSetup から呼ぶ)
    // =========================================================================

    public static void registerUpgrades() {
        var card = ModItems.WEN_WIRELESS_ENERGYCARD.get();
        LOGGER.info("[WEN-AE2] registerUpgrades: card item = {}", card);
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
        Set<BlockPos> set = CANDIDATES.computeIfAbsent(sl.dimension(),
                k -> ConcurrentHashMap.newKeySet());
        int added = 0;
        for (var entry : chunk.getBlockEntities().entrySet()) {
            BlockEntity be = entry.getValue();
            if (be instanceof IUpgradeableObject || be instanceof IPartHost) {
                set.add(entry.getKey());
                added++;
            }
        }
        if (added > 0) {
            LOGGER.debug("[WEN-AE2] ChunkLoad @{} {}: {} candidate BE(s) added (total: {})",
                    sl.dimension().location(), chunk.getPos(), added, set.size());
        }
    }

    @SubscribeEvent
    public static void onChunkUnload(ChunkEvent.Unload event) {
        if (!(event.getLevel() instanceof ServerLevel sl)) return;
        if (!(event.getChunk() instanceof LevelChunk chunk)) return;
        Set<BlockPos> set = CANDIDATES.get(sl.dimension());
        if (set == null) return;
        for (var pos : chunk.getBlockEntities().keySet()) {
            set.remove(pos);
        }
    }

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
        // 全てのpoll呼び出しで候補数をログ (info)
        int candidateCount = candidates == null ? 0 : candidates.size();
        boolean verbose = (++logTickCounter % 10 == 0);
        if (candidates == null || candidates.isEmpty()) {
            return;
        }

        int totalScanned = 0;
        int withCard = 0;
        int boundCards = 0;
        int gridResolved = 0;
        String lastNetworkId = null;
        BlockPos lastCardPos = null;

        // grid → networkId (first-wins で重複整理)
        Map<IGrid, String> gridToNetwork = new HashMap<>();

        for (BlockPos pos : candidates) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be == null) continue;
            totalScanned++;

            // (1) BE 自身がIUpgradeableObject
            if (be instanceof IUpgradeableObject upgradeable) {
                ItemStack card = findCard(upgradeable.getUpgrades());
                if (!card.isEmpty()) {
                    withCard++;
                    lastCardPos = pos;
                    String networkId = WENEnergyCardItem.getBoundNetworkId(card);
                    LOGGER.debug("[WEN-AE2] Card in BE@{} ({}), networkId='{}'",
                            pos, be.getClass().getSimpleName(), networkId);
                    if (!networkId.isEmpty()) {
                        boundCards++;
                        lastNetworkId = networkId;
                        IGrid grid = resolveGrid(be);
                        if (grid != null) {
                            gridResolved++;
                            gridToNetwork.putIfAbsent(grid, networkId);
                            WARNED_NO_GRID.remove(pos);
                        } else if (WARNED_NO_GRID.add(pos)) {
                            LOGGER.warn("[WEN-AE2] BE@{} ({}) has bound card but NO grid — not connected to ME network?",
                                    pos, be.getClass().getSimpleName());
                        }
                    }
                }
            }

            // (2) BE が IPartHost なら各パートもチェック
            if (be instanceof IPartHost partHost) {
                for (Direction d : Direction.values()) {
                    IPart part = partHost.getPart(d);
                    if (!(part instanceof IUpgradeableObject upPart)) continue;
                    ItemStack card = findCard(upPart.getUpgrades());
                    if (card.isEmpty()) continue;
                    withCard++;
                    lastCardPos = pos;
                    String networkId = WENEnergyCardItem.getBoundNetworkId(card);
                    LOGGER.debug("[WEN-AE2] Card in PART@{} dir={} ({}), networkId='{}'",
                            pos, d, part.getClass().getSimpleName(), networkId);
                    if (networkId.isEmpty()) continue;
                    boundCards++;
                    lastNetworkId = networkId;
                    IGrid grid = resolveGrid(be);
                    if (grid != null) {
                        gridResolved++;
                        gridToNetwork.putIfAbsent(grid, networkId);
                    }
                }
                IPart centerPart = partHost.getPart((Direction) null);
                if (centerPart instanceof IUpgradeableObject upPart) {
                    ItemStack card = findCard(upPart.getUpgrades());
                    if (!card.isEmpty()) {
                        withCard++;
                        lastCardPos = pos;
                        String networkId = WENEnergyCardItem.getBoundNetworkId(card);
                        if (!networkId.isEmpty()) {
                            boundCards++;
                            lastNetworkId = networkId;
                            IGrid grid = resolveGrid(be);
                            if (grid != null) {
                                gridResolved++;
                                gridToNetwork.putIfAbsent(grid, networkId);
                            }
                        }
                    }
                }
            }
        }

        // 統計: verbose (10秒毎) 時のみINFO、それ以外の間もカード変化あればDEBUG
        if (verbose) {
            LOGGER.info("[WEN-AE2] {}: candidates={}, scanned={}, withCard={}, bound={}, gridResolved={}, uniqueGrids={}",
                    level.dimension().location(), candidateCount, totalScanned, withCard, boundCards, gridResolved, gridToNetwork.size());
        } else if (withCard > 0) {
            LOGGER.debug("[WEN-AE2] {}: bound={}, gridResolved={}", level.dimension().location(), boundCards, gridResolved);
        }

        if (gridToNetwork.isEmpty()) return;
        WENNetworkData data = WENNetworkData.get(server.overworld());

        for (var entry : gridToNetwork.entrySet()) {
            supplyGrid(entry.getKey(), entry.getValue(), data, verbose);
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

    // =========================================================================
    //  実際の給電: AE2 grid.energyService.injectPower で注入、WENから消費
    // =========================================================================

    private static void supplyGrid(IGrid grid, String networkId, WENNetworkData data, boolean verbose) {
        WENNetworkData.WENEntry entry = data.getNetwork(networkId);
        if (entry == null) {
            // networkId不整合 (モニター破壊後にカードだけ残ってる等) — 稀なので毎回WARN
            LOGGER.warn("[WEN-AE2] Network '{}' not found (card bound to deleted network?)", networkId);
            return;
        }
        if (entry.storedEnergy.signum() <= 0) return;

        IEnergyService es = grid.getEnergyService();
        double simLeftover = es.injectPower(MAX_AE_PER_POLL, Actionable.SIMULATE);
        double aeAcceptable = MAX_AE_PER_POLL - simLeftover;
        if (aeAcceptable < AE_PER_EU) return;

        long euRequested = (long) (aeAcceptable / AE_PER_EU);
        long euAvailable = entry.storedEnergy.min(BigInteger.valueOf(euRequested)).longValue();
        if (euAvailable <= 0) return;

        double aeToSend = euAvailable * AE_PER_EU;
        double finalLeftover = es.injectPower(aeToSend, Actionable.MODULATE);
        long euInjected = (long) ((aeToSend - finalLeftover) / AE_PER_EU);
        if (euInjected > 0) {
            data.removeEnergy(networkId, euInjected);
            if (verbose) LOGGER.info("[WEN-AE2] Supplied '{}': {} AE ({} EU), WEN remaining={}",
                    networkId, (aeToSend - finalLeftover), euInjected, entry.storedEnergy);
        }
    }
}