package DIV.gtcsolo.combat;

import DIV.gtcsolo.registry.ModAttributes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.EntityAttributeModificationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Lucky Hit attribute を全 LivingEntity に attach する handler。
 *
 * <p>mod event bus 専用 ({@link EntityAttributeModificationEvent} は IModBusEvent)。
 * forge bus 側の処理 (LivingDamageEvent) は {@link LuckyHitHandler} へ分離 — 1 クラスを
 * 両 bus に register するのは Forge 制約上 NG (= mod bus が forge event を見つけて拒否する)。
 */
public class LuckyHitAttribute {

    @SubscribeEvent
    public void onAttributeModification(EntityAttributeModificationEvent event) {
        for (EntityType<? extends LivingEntity> type : event.getTypes()) {
            if (!event.has(type, ModAttributes.LUCKY_HIT_RATE.get())) {
                event.add(type, ModAttributes.LUCKY_HIT_RATE.get());
            }
            if (!event.has(type, ModAttributes.LUCKY_HIT_DAMAGE.get())) {
                event.add(type, ModAttributes.LUCKY_HIT_DAMAGE.get());
            }
        }
    }
}
