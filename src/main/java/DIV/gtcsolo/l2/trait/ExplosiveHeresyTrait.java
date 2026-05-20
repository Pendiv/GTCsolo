package DIV.gtcsolo.l2.trait;

import DIV.gtcsolo.l2.trait.base.TypedMobTrait;
import net.minecraft.ChatFormatting;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

/**
 * [39] Explosive Heresy (爆発の邪道) — クリーパー専用。 自爆以外の爆発で受けるダメージが
 * 2.5 倍になる (レベル不変)。
 *
 * <p>自爆判定: damage source の {@link DamageSource#getEntity()} が自身かどうかで判別。
 */
public class ExplosiveHeresyTrait extends TypedMobTrait {

    private static final float HERESY_MULT = 2.5f;

    public ExplosiveHeresyTrait(ChatFormatting style) {
        super(style);
    }

    @Override
    protected boolean isValidTarget(LivingEntity mob) {
        return mob instanceof Creeper;
    }

    @Override
    protected void onValidHurtByOthers(int level, LivingEntity entity, LivingHurtEvent event) {
        DamageSource src = event.getSource();
        if (!src.is(DamageTypeTags.IS_EXPLOSION)) return;
        if (src.getEntity() == entity) return; // 自爆は対象外
        event.setAmount(event.getAmount() * HERESY_MULT);
    }
}
