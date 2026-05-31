package DIV.gtcsolo.progression;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerXpEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * 恒久強化データの配線 (Forge event bus):
 * <ul>
 *   <li>{@link AttachCapabilitiesEvent} — player に {@link PlayerProgressionProvider} を付与</li>
 *   <li>{@link PlayerEvent.Clone} — 死亡/次元移動で新 entity へデータ引き継ぎ</li>
 *   <li>{@link PlayerXpEvent.XpChange} — 獲得XPを累積しレベル/ポイントを更新</li>
 *   <li>{@link PlayerEvent.PlayerLoggedInEvent} — 既存キャラを現在XPで一度だけ初期化</li>
 * </ul>
 */
public class ProgressionEvents {

    @SubscribeEvent
    public void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(ProgressionNodes.INSTANCE);
    }

    @SubscribeEvent
    public void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player
                && !event.getCapabilities().containsKey(PlayerProgressionProvider.ID)) {
            event.addCapability(PlayerProgressionProvider.ID, new PlayerProgressionProvider());
        }
    }

    @SubscribeEvent
    public void onClone(PlayerEvent.Clone event) {
        Player original = event.getOriginal();
        original.reviveCaps();
        original.getCapability(ProgressionCapability.PLAYER).ifPresent(old ->
                event.getEntity().getCapability(ProgressionCapability.PLAYER)
                        .ifPresent(now -> now.load(old.save())));
        original.invalidateCaps();
    }

    @SubscribeEvent
    public void onXpChange(PlayerXpEvent.XpChange event) {
        int amount = event.getAmount();
        if (amount <= 0) return;
        if (event.getEntity().level().isClientSide()) return;
        event.getEntity().getCapability(ProgressionCapability.PLAYER)
                .ifPresent(d -> d.addXp(amount));
    }

    @SubscribeEvent
    public void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        Player p = event.getEntity();
        if (p.level().isClientSide()) return;
        p.getCapability(ProgressionCapability.PLAYER).ifPresent(d -> {
            d.seedIfNeeded(currentTotalXp(p));
            ProgressionAttributes.reapply(p, d);   // transient modifier を貼り直す
        });
    }

    @SubscribeEvent
    public void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        Player p = event.getEntity();
        if (p.level().isClientSide()) return;
        // 新 entity は attribute が初期化されるため貼り直す (データは Clone で引き継ぎ済み)
        p.getCapability(ProgressionCapability.PLAYER)
                .ifPresent(d -> ProgressionAttributes.reapply(p, d));
    }

    /** player の現在の累積XP (レベル + 進捗バー) を算出。 */
    private static long currentTotalXp(Player p) {
        long total = PlayerProgression.totalXpToReach(p.experienceLevel);
        total += Math.round(p.experienceProgress * p.getXpNeededForNextLevel());
        return total;
    }
}
