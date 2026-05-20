package DIV.gtcsolo.l2;

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
        }
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

    /** HomingShot: arrow 出現時にマーク */
    @SubscribeEvent
    public void onEntityJoinLevel(EntityJoinLevelEvent event) {
        HomingShotTrait.onArrowJoin(event);
    }

    /** FullTank: 爆発を拡大版 + fire 化で打ち直し */
    @SubscribeEvent
    public void onExplosionStart(ExplosionEvent.Start event) {
        FullTankTrait.onExplosionStart(event);
    }

    /** VolatileMix: 爆発後に残留デバフ雲生成 */
    @SubscribeEvent
    public void onExplosionDetonate(ExplosionEvent.Detonate event) {
        VolatileMixTrait.onExplosionDetonate(event);
    }
}
