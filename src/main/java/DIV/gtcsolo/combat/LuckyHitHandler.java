package DIV.gtcsolo.combat;

import DIV.gtcsolo.registry.ModAttributes;
import com.mojang.logging.LogUtils;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.slf4j.Logger;

/**
 * Lucky Hit の発動 handler (forge bus 専用)。
 *
 * <p>{@link LivingHurtEvent} を {@link EventPriority#LOWEST} で listen — 同 event の他全 listener
 * (= L2DT crit 系 addHurtModifier / addDealtModifier、 Apoth attribute crit、 trait の修正 etc.) が
 * 走り終わった後に最後に発火、 amount に倍率を掛ける。
 *
 * <p>vanilla actuallyHurt のパイプライン上では LivingHurtEvent → armor 軽減 → magic_absorb →
 * absorption 差し引き → LivingDamageEvent → setHealth の順なので、 ここで乗算すれば
 * <b>「全 crit / mod 後、 armor 軽減前」</b> の段階で倍率がかかる (= 防御で 99% 削られる場合でも
 * 乗算が表示ダメージに反映されて visible になる)。
 *
 * <p>mod bus 側 (attribute attach) は {@link LuckyHitAttribute} へ分離。
 */
public class LuckyHitHandler {
    private static final Logger LOGGER = LogUtils.getLogger();

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onLivingHurt(LivingHurtEvent event) {
        if (event.isCanceled()) return;
        Entity src = event.getSource().getEntity();
        if (!(src instanceof LivingEntity attacker)) return;
        AttributeInstance rateInst = attacker.getAttribute(ModAttributes.LUCKY_HIT_RATE.get());
        if (rateInst == null) return;
        double rate = rateInst.getValue();
        if (rate <= 0) return;
        if (attacker.getRandom().nextDouble() >= rate) return;

        AttributeInstance dmgInst = attacker.getAttribute(ModAttributes.LUCKY_HIT_DAMAGE.get());
        double dmgPct = dmgInst != null ? dmgInst.getValue() : 100.0;
        double mult = dmgPct / 100.0;
        if (mult == 1.0) return; // 等倍 = no buff

        float before = event.getAmount();
        float after = before * (float) mult;
        event.setAmount(after);
        LOGGER.info("[LuckyHit] {} → {} (rate={}, mult={}x)",
                before, after, rate, mult);
    }
}
