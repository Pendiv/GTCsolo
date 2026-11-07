package DIV.gtcsolo.performance;

import com.mojang.logging.LogUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/** Observes player tracking loss without repairing entities, loading chunks, or changing game rules. */
@Mod.EventBusSubscriber(modid = "gtcsolo")
public final class PlayerTrackingDiagnostics {
    private static final Logger LOGGER = LogUtils.getLogger();
    // Only accessed on the server thread; retain one entry per currently affected online player.
    private static final Map<UUID, Registration> REPORTED = new HashMap<>();
    private static boolean inspectionFailureReported;

    private PlayerTrackingDiagnostics() {}

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        REPORTED.clear();
        inspectionFailureReported = false;
        LOGGER.info("[PlayerTracking] Diagnostics active: removal stacks and registration checks every 20 ticks");
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        REPORTED.clear();
        inspectionFailureReported = false;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void onJoin(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || !(event.getEntity() instanceof ServerPlayer player) || player instanceof FakePlayer) return;
        try {
            // This event precedes registration. Missing lookups here do not imply a fault.
            if (event.isCanceled()) {
                LOGGER.warn("[PlayerTracking] JOIN_CANCELED {}", describe(player, level),
                        new Throwable("Diagnostic join dispatch stack (not thrown; cancellation source may differ)"));
            } else {
                LOGGER.info("[PlayerTracking] JOIN_REQUEST {}", describe(player, level));
            }
        } catch (RuntimeException error) {
            LOGGER.warn("[PlayerTracking] Could not inspect player join", error);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLeave(EntityLeaveLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || !(event.getEntity() instanceof ServerPlayer player) || player instanceof FakePlayer) return;
        try {
            // Forge dispatches this synchronously from onTrackingEnd: keep the original removal caller.
            // Normal logout, respawn and dimension transfers also appear here and are not errors.
            LOGGER.info("[PlayerTracking] LEAVE {}", describe(player, level),
                    new Throwable("Diagnostic player tracking removal stack (not thrown)"));
        } catch (RuntimeException error) {
            LOGGER.warn("[PlayerTracking] Could not inspect player removal", error);
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % 20 != 0) return;
        try {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (player instanceof FakePlayer || player.connection == null
                        || !player.connection.connection.isConnected()) continue;
                Registration registration = registration(player, player.serverLevel());
                PlayerTrackingReproduction.onSample(player, registration);
                if (registration.healthy()) {
                    if (REPORTED.remove(player.getUUID()) != null) {
                        LOGGER.info("[PlayerTracking] RECOVERED {}", describe(player, player.serverLevel()));
                    }
                } else {
                    Registration previous = REPORTED.put(player.getUUID(), registration);
                    if (!registration.equals(previous)) {
                        LOGGER.warn("[PlayerTracking] REGISTRATION_MISMATCH {}", describe(player, player.serverLevel()));
                    }
                }
            }
            REPORTED.keySet().removeIf(uuid -> server.getPlayerList().getPlayer(uuid) == null);
            inspectionFailureReported = false;
        } catch (RuntimeException error) {
            // A diagnostic failure must not interrupt a world tick.
            if (!inspectionFailureReported) {
                inspectionFailureReported = true;
                LOGGER.warn("[PlayerTracking] Could not inspect player registration", error);
            }
        }
    }

    static Registration registration(ServerPlayer player, ServerLevel level) {
        boolean inLevelPlayers = false;
        for (ServerPlayer listed : level.players()) {
            if (listed == player) {
                inLevelPlayers = true;
                break;
            }
        }
        return new Registration(level.dimension().location().toString(), System.identityHashCode(player),
                player.getId(), player.isRemoved(), level.getServer().getPlayerList().getPlayer(player.getUUID()) == player,
                inLevelPlayers, level.getEntity(player.getUUID()) == player, level.getEntity(player.getId()) == player,
                player.connection != null && player.connection.player == player);
    }

    static String describe(ServerPlayer player, ServerLevel level) {
        MinecraftServer server = level.getServer();
        String basic = String.format(Locale.ROOT,
                "name=%s uuid=%s entityId=%d identity=%x dimension=%s pos=(%.2f,%.2f,%.2f) "
                        + "removed=%s reason=%s health=%.1f playerTicks=%d connected=%s thread=%s serverThread=%s",
                player.getGameProfile().getName(), player.getUUID(), player.getId(), System.identityHashCode(player),
                level.dimension().location(), player.getX(), player.getY(), player.getZ(), player.isRemoved(),
                player.getRemovalReason(), player.getHealth(), player.tickCount,
                player.connection != null && player.connection.connection.isConnected(),
                Thread.currentThread().getName(), server.isSameThread());
        // In case a mod removes a server player on the wrong thread, capture its stack without traversing world maps.
        if (!server.isSameThread()) return basic;
        var chunkPos = player.chunkPosition();
        var chunk = level.getChunkSource().getChunkNow(chunkPos.x, chunkPos.z); // Never load/generate chunks.
        return basic + " serverTicks=" + server.getTickCount() + " gameTime=" + level.getGameTime()
                + " registration=" + registration(player, level)
                + " uuidLookup=" + reference(level.getEntity(player.getUUID()))
                + " idLookup=" + reference(level.getEntity(player.getId()))
                + " chunk=" + chunkPos + " chunkStatus=" + (chunk == null ? "absent" : chunk.getFullStatus())
                + " blockTicking=" + level.shouldTickBlocksAt(chunkPos.toLong());
    }

    private static String reference(Entity entity) {
        return entity == null ? "null" : entity.getClass().getName() + "@"
                + Integer.toHexString(System.identityHashCode(entity)) + ":" + entity.getId();
    }

    record Registration(String dimension, int identity, int entityId, boolean removed,
                                boolean onlinePlayer, boolean levelPlayer, boolean byUuid,
                                boolean byId, boolean connectionPlayer) {
        boolean healthy() {
            return !removed && onlinePlayer && levelPlayer && byUuid && byId && connectionPlayer;
        }
    }
}
