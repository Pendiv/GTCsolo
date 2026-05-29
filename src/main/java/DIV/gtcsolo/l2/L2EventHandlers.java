package DIV.gtcsolo.l2;

import DIV.gtcsolo.l2.trait.AccumulationTrait;
import DIV.gtcsolo.l2.trait.ArroganceTrait;
import DIV.gtcsolo.l2.trait.DragonicHeartTrait;
import DIV.gtcsolo.l2.trait.EqualTrait;
import DIV.gtcsolo.l2.trait.FullTankTrait;
import DIV.gtcsolo.l2.trait.HomingShotTrait;
import DIV.gtcsolo.l2.trait.PureHeartTrait;
import DIV.gtcsolo.l2.trait.ResentmentTrait;
import DIV.gtcsolo.l2.trait.VolatileMixTrait;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * gtcsolo 独自 L2Hostility trait のうち、 MobTrait の hook だけでは扱えないイベントを listen する handler。
 *
 * <ul>
 *   <li>{@link LivingHealEvent} → ArroganceTrait の回復阻害</li>
 *   <li>{@link LivingChangeTargetEvent} → EqualTrait のターゲット確定タイマー</li>
 *   <li>{@link LivingDamageEvent} → ResentmentTrait の被弾累積</li>
 *   <li>{@link EntityLeaveLevelEvent} → DragonicHeartTrait のクリスタル破壊検知</li>
 * </ul>
 */
public class L2EventHandlers {

    @SubscribeEvent
    public void onHeal(LivingHealEvent event) {
        LivingEntity entity = event.getEntity();
        if (ArroganceTrait.shouldBlockHeal(entity)) {
            event.setCanceled(true);
            return;
        }
        // 隠蔽 (Concealment): MobEffect 由来でない回復で level 減少
        DIV.gtcsolo.l2.effect.ConcealmentEffect.onHeal(entity, event.getAmount());
    }

    @SubscribeEvent
    public void onChangeTarget(LivingChangeTargetEvent event) {
        if (event.getNewTarget() == null) return;
        if (!(event.getNewTarget() instanceof net.minecraft.world.entity.player.Player)) return;
        EqualTrait.onTargetLocked(event.getEntity());
    }

    @SubscribeEvent
    public void onDamaged(LivingDamageEvent event) {
        ResentmentTrait.onDamaged(event.getEntity(), event);
    }

    @SubscribeEvent
    public void onEntityLeave(EntityLeaveLevelEvent event) {
        DragonicHeartTrait.onEntityLeave(event);
    }

    /** 時空 trait の死亡フック集約: 骨拾い buff / 無限再帰 player キャリア再移譲 / 英雄 count */
    @SubscribeEvent
    public void onDeath(net.minecraftforge.event.entity.living.LivingDeathEvent event) {
        LivingEntity dead = event.getEntity();
        DIV.gtcsolo.l2.trait.SpacetimeBonePickerTrait.onAnyDeath(dead);
        DIV.gtcsolo.l2.trait.SpacetimeInfiniteRecursionTrait.onAnyDeath(dead);
        DIV.gtcsolo.l2.trait.SpacetimeHeroTrait.onAnyDeath(dead);
        DIV.gtcsolo.l2.trait.SpacetimeChainOfCausalityTrait.onAnyDeath(event);
    }

    /** DragonicHeart の per-tick 処理 + HomingShot の trajectory 補正 */
    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        DragonicHeartTrait.serverTick(event.getServer());
        HomingShotTrait.serverTick(event.getServer());
    }

    /** PureHeart: harmful effect 付与を DENY する */
    @SubscribeEvent
    public void onEffectApplicable(MobEffectEvent.Applicable event) {
        PureHeartTrait.onApplicable(event);
    }

    /** HomingShot: arrow 出現時にマーク。 Spacetime Diffusion: 新規 spawn mob へ確率付与 */
    @SubscribeEvent
    public void onEntityJoinLevel(EntityJoinLevelEvent event) {
        HomingShotTrait.onArrowJoin(event);
        DIV.gtcsolo.l2.trait.SpacetimeDiffusionTrait.onMobSpawn(event);
    }

    /** FullTank: 爆発を拡大版 + fire 化で打ち直し。 Accumulation: 被弾蓄積で拡大 (= FullTank 未 cancel 時のみ) */
    @SubscribeEvent
    public void onExplosionStart(ExplosionEvent.Start event) {
        FullTankTrait.onExplosionStart(event);
        if (!event.isCanceled()) {
            AccumulationTrait.onExplosionStart(event);
        }
    }

    /** VolatileMix: 爆発後に残留デバフ雲生成 */
    @SubscribeEvent
    public void onExplosionDetonate(ExplosionEvent.Detonate event) {
        VolatileMixTrait.onExplosionDetonate(event);
    }
}
