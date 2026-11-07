package DIV.gtcsolo.performance;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.logging.LogUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Deliberate, operator-only reproduction; automatic observations never remove or repair a player. */
@Mod.EventBusSubscriber(modid = "gtcsolo")
public final class PlayerTrackingReproduction {
    private static final Logger LOGGER = LogUtils.getLogger();
    // Retain data, not player/server objects, across title-screen relogs in the same JVM.
    private static final Map<WorldPlayer, RejoinTrace> PENDING = new LinkedHashMap<>();
    private static final int MAX_PENDING = 8;

    private PlayerTrackingReproduction() {}

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("gtcsolo")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("player_tracking")
                        .executes(context -> status(context.getSource()))
                        .then(Commands.literal("status").executes(context -> status(context.getSource())))
                        .then(Commands.literal("reproduce").executes(context -> reproduce(context.getSource())))));
    }

    private static int status(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        boolean healthy = PlayerTrackingDiagnostics.registration(player, player.serverLevel()).healthy();
        LOGGER.info("[PlayerTracking] STATUS {}", PlayerTrackingDiagnostics.describe(player, player.serverLevel()));
        source.sendSuccess(() -> Component.literal("プレイヤー登録: " + (healthy ? "正常" : "異常")
                + "。詳細をログへ記録しました。"), false);
        return healthy ? 1 : 0;
    }

    private static int reproduce(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (player instanceof FakePlayer || !PlayerTrackingDiagnostics.registration(player, player.serverLevel()).healthy()) {
            source.sendFailure(Component.literal("登録が正常なプレイヤーで実行してください。既に異常な状態へは再実行しません。"));
            return 0;
        }
        remember(player, "gtcsolo:reproduce");
        LOGGER.warn("[PlayerTracking] REPRODUCE_BEGIN origin=gtcsolo {}",
                PlayerTrackingDiagnostics.describe(player, player.serverLevel()));
        // Keep the connection/player-list entry. Only this explicit command removes the world entity.
        player.discard();
        LOGGER.warn("[PlayerTracking] REPRODUCE_AFTER origin=gtcsolo {}",
                PlayerTrackingDiagnostics.describe(player, player.serverLevel()));
        source.sendSuccess(() -> Component.literal("登録消失の再現処理を実行しました。症状を確認した後、タイトルへ戻って同じワールドへ入り直すと比較結果を表示します。"), false);
        return 1;
    }

    static void onSample(ServerPlayer player, PlayerTrackingDiagnostics.Registration registration) {
        WorldPlayer key = key(player);
        RejoinTrace trace = PENDING.get(key);
        if (!registration.healthy() && trace == null) {
            trace = remember(player, "observed");
        }
        if (trace == null || trace.reportAfterTick < 0
                || player.server.getTickCount() < trace.reportAfterTick) return;
        trace.reportAfterTick = -1;
        PlayerState current = PlayerState.capture(player);
        PlayerState reference = trace.atLogout == null ? trace.atFailure : trace.atLogout;
        String comparisonPoint = trace.atLogout == null ? "発症記録時" : "退出時";
        List<String> changes = reference.changesFrom(current);
        LOGGER.info("[PlayerTracking] REJOIN origin={} comparison={} changes={} before={} after={} {}",
                trace.origin, comparisonPoint, changes, reference.summary(), current.summary(),
                PlayerTrackingDiagnostics.describe(player, player.serverLevel()));
        player.sendSystemMessage(Component.literal("プレイヤー登録診断: 再入場後の登録は"
                + (registration.healthy() ? "正常" : "異常のまま") + "。" + comparisonPoint + "から"
                + (changes.isEmpty() ? "比較項目に差分なし。" : "の変化: " + String.join("、", changes) + "。詳細はログに記録しました。")));
        if (registration.healthy()) PENDING.remove(key);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player instanceof FakePlayer) return;
        try {
            RejoinTrace trace = PENDING.get(key(player));
            if (trace != null) {
                trace.atLogout = PlayerState.capture(player);
                trace.reportAfterTick = -1;
                LOGGER.info("[PlayerTracking] LOGOUT_BASELINE origin={} state={}", trace.origin, trace.atLogout.summary());
            }
        } catch (RuntimeException error) {
            LOGGER.warn("[PlayerTracking] Could not capture relog baseline", error);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player instanceof FakePlayer) return;
        try {
            RejoinTrace trace = PENDING.get(key(player));
            if (trace != null) trace.reportAfterTick = (long) player.server.getTickCount() + 20;
        } catch (RuntimeException error) {
            LOGGER.warn("[PlayerTracking] Could not schedule relog comparison", error);
        }
    }

    private static RejoinTrace remember(ServerPlayer player, String origin) {
        WorldPlayer key = key(player);
        RejoinTrace trace = new RejoinTrace(origin, PlayerState.capture(player));
        PENDING.remove(key);
        PENDING.put(key, trace);
        while (PENDING.size() > MAX_PENDING) PENDING.remove(PENDING.keySet().iterator().next());
        LOGGER.info("[PlayerTracking] COMPARISON_BASELINE origin={} state={}", origin, trace.atFailure.summary());
        return trace;
    }

    private static WorldPlayer key(ServerPlayer player) {
        return new WorldPlayer(player.server.getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize().toString(),
                player.getUUID());
    }

    private record WorldPlayer(String savePath, UUID playerId) {}

    private static final class RejoinTrace {
        final String origin;
        final PlayerState atFailure;
        PlayerState atLogout;
        long reportAfterTick = -1;

        RejoinTrace(String origin, PlayerState atFailure) {
            this.origin = origin;
            this.atFailure = atFailure;
        }
    }

    private record PlayerState(ListTag inventory, float health, int food, int experience,
                               int experienceLevel, float experienceProgress, String gameMode,
                               String dimension, Vec3 position) {
        static PlayerState capture(ServerPlayer player) {
            return new PlayerState(player.getInventory().save(new ListTag()), player.getHealth(),
                    player.getFoodData().getFoodLevel(), player.totalExperience, player.experienceLevel,
                    player.experienceProgress, player.gameMode.getGameModeForPlayer().getName(),
                    player.level().dimension().location().toString(), player.position());
        }

        List<String> changesFrom(PlayerState other) {
            List<String> changes = new ArrayList<>();
            if (!inventory.equals(other.inventory)) changes.add("持ち物の内容・状態");
            if (Float.compare(health, other.health) != 0) changes.add("体力");
            if (food != other.food) changes.add("空腹度");
            if (experience != other.experience || experienceLevel != other.experienceLevel
                    || Float.compare(experienceProgress, other.experienceProgress) != 0) changes.add("経験値");
            if (!gameMode.equals(other.gameMode)) changes.add("ゲームモード");
            if (!dimension.equals(other.dimension)) changes.add("ディメンション");
            if (position.distanceToSqr(other.position) > 0.0001) changes.add("位置");
            return changes;
        }

        String summary() {
            return "inventoryHash=" + inventory.hashCode() + " health=" + health + " food=" + food
                    + " xp=" + experience + "/" + experienceLevel + "/" + experienceProgress
                    + " mode=" + gameMode + " dimension=" + dimension + " pos=" + position;
        }
    }
}
