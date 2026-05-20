package DIV.gtcsolo.combat.curtaincall;

import com.mojang.logging.LogUtils;
import dev.xkmc.l2damagetracker.init.L2DamageTracker;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.slf4j.Logger;

/**
 * 激戦の幕引きの Forge event listener。
 *
 * <ul>
 *   <li>PlayerTickEvent: 毎 tick {@link CurtainCall#tick} 呼んでスタック増減</li>
 *   <li>LivingHurtEvent (HIGH): player ↔ hostile mob のダメージ記録 + 効果 1/2 適用</li>
 *   <li>LivingHealEvent: 効果 3 (heal 減衰)</li>
 *   <li>PlayerLoggedOutEvent / LivingDeathEvent (player): state クリア</li>
 * </ul>
 */
public class CurtainCallHandler {
    private static final Logger LOGGER = LogUtils.getLogger();

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.player.level().isClientSide()) return;
        CurtainCall.tick(event.player, event.player.level().getGameTime());
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntity().level().isClientSide()) return;
        LivingEntity target = event.getEntity();
        var source = event.getSource();
        var srcEntity = source.getEntity();
        long gameTime = target.level().getGameTime();

        Player player = null;
        LivingEntity hostile = null;
        boolean playerAsAttacker;

        if (target instanceof Player p && srcEntity instanceof LivingEntity le && isHostile(le)) {
            player = p;
            hostile = le;
            playerAsAttacker = false;
        } else if (srcEntity instanceof Player p && isHostile(target)) {
            player = p;
            hostile = target;
            playerAsAttacker = true;
        } else {
            return;
        }

        // ダメージ記録 (= 膠着判定の元データ + 戦闘相手 tracking)
        if (playerAsAttacker) {
            CurtainCall.onDealtToHostile(player, hostile, gameTime);
        } else {
            CurtainCall.onTakenFromHostile(player, hostile, gameTime);
        }

        int stacks = CurtainCall.getStacks(player);
        if (stacks <= 0) return;

        // 効果 1: 与ダメ増加 (= 双方対称、 攻撃側に依らず player 戦闘単位で乗算)
        double multiplier = CurtainCall.damageMultiplier(stacks);

        // 効果 2: 軽減減衰 — target の damage_reduction 属性を緩和
        AttributeInstance reductionInst = target.getAttribute(L2DamageTracker.REDUCTION.get());
        if (reductionInst != null) {
            double attr = reductionInst.getValue();
            double reductionMult = CurtainCall.reductionAttrMultiplier(attr, stacks);
            multiplier *= reductionMult;
        }

        if (multiplier != 1.0) {
            float before = event.getAmount();
            event.setAmount(before * (float) multiplier);
            LOGGER.info("[CurtainCall] {} stacks={}, dmg {} -> {} (mult {})",
                    player.getName().getString(), stacks, before, event.getAmount(), multiplier);
        }
    }

    @SubscribeEvent
    public void onLivingHeal(LivingHealEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide()) return;

        Player relevantPlayer = null;
        if (entity instanceof Player p) {
            relevantPlayer = p;
        } else if (entity instanceof Mob && isHostile(entity)) {
            // hostile mob の heal: 半径 16 内の最寄り Player を関連 player とする (= 簡易判定)
            relevantPlayer = entity.level().getNearestPlayer(entity, 16);
        }
        if (relevantPlayer == null) return;

        int stacks = CurtainCall.getStacks(relevantPlayer);
        if (stacks <= 0) return;

        double decay = CurtainCall.healDecay(stacks);
        if (decay <= 0) return;

        float before = event.getAmount();
        float after = before * (float) (1.0 - decay);
        event.setAmount(after);
        LOGGER.info("[CurtainCall] {} stacks={}, heal on {} {} -> {} (decay {})",
                relevantPlayer.getName().getString(), stacks, entity, before, after, decay);
    }

    @SubscribeEvent
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        CurtainCall.clear(event.getEntity());
    }

    @SubscribeEvent
    public void onLivingDeath(LivingDeathEvent event) {
        LivingEntity dying = event.getEntity();
        if (dying instanceof Player p) {
            CurtainCall.clear(p);
            return;
        }
        // ボス mob 死亡 → 該当 player の state リセット
        if (dying.level().isClientSide()) return;
        CurtainCall.handleMobDeath(dying);
    }

    /** 簡易 hostile 判定: Enemy 実装するか、 Mob.getTarget が Player */
    private static boolean isHostile(LivingEntity entity) {
        if (entity instanceof Enemy) return true;
        if (entity instanceof Mob m && m.getTarget() instanceof Player) return true;
        return false;
    }
}
