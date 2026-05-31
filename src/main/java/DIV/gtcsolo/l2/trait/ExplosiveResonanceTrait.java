package DIV.gtcsolo.l2.trait;

import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import net.minecraft.ChatFormatting;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

/**
 * [27] Explosive Resonance — 爆発ダメージを受けると、 代わりに回復する。
 *
 * <p>爆発物による範囲攻略を逆用、 爆破では倒せない相手にする。
 */
public class ExplosiveResonanceTrait extends MobTrait {

    public ExplosiveResonanceTrait(ChatFormatting style) {
        super(style);
    }

    @Override
    public void onHurtByOthers(int level, LivingEntity entity, LivingHurtEvent event) {
        if (!event.getSource().is(DamageTypeTags.IS_EXPLOSION)) return;
        float heal = event.getAmount();
        event.setCanceled(true);
        entity.heal(heal);  // heal() は内部で ForgeEventFactory.onLivingHeal を尊重し maxHP で cap
    }
}
