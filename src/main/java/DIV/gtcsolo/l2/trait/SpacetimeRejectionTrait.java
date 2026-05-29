package DIV.gtcsolo.l2.trait;

import DIV.gtcsolo.l2.SpacetimeTraits;
import DIV.gtcsolo.l2.trait.base.ISpacetimeTrait;
import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import net.minecraft.ChatFormatting;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

/**
 * 時空の排斥 (Spacetime Rejection) — 時空タイプを持たない mob / player に継続的な小ダメージを与える。
 * ダメージは自身の攻撃力を参照し、 level が上がるほど範囲も参照割合も増える。
 *
 * <p>1 秒おきに自身を中心とする AABB 内を走査。 player は常に対象、 mob は
 * {@link SpacetimeTraits#isSpacetimeMob} が false の場合のみ対象。
 */
public class SpacetimeRejectionTrait extends MobTrait implements ISpacetimeTrait {

    private static final int INTERVAL = 20;
    private static final double RADIUS_BASE = 4.0;
    private static final double RADIUS_PER_LEVEL = 1.0;
    private static final float FRAC_BASE = 0.10f;
    private static final float FRAC_PER_LEVEL = 0.05f;
    private static final float FALLBACK_ATK = 2.0f;

    public SpacetimeRejectionTrait(ChatFormatting style) {
        super(style);
    }

    @Override
    public void tick(LivingEntity mob, int level) {
        super.tick(mob, level);
        if (mob.level().isClientSide()) return;
        if (mob.tickCount % INTERVAL != 0) return;
        double radius = RADIUS_BASE + RADIUS_PER_LEVEL * level;
        float frac = FRAC_BASE + FRAC_PER_LEVEL * level;
        AttributeInstance atkInst = mob.getAttribute(Attributes.ATTACK_DAMAGE);
        float atk = atkInst != null ? (float) atkInst.getValue() : FALLBACK_ATK;
        float dmg = atk * frac;
        if (dmg <= 0f) return;
        DamageSource src = mob.damageSources().magic();
        AABB area = mob.getBoundingBox().inflate(radius);
        for (LivingEntity victim : mob.level().getEntitiesOfClass(LivingEntity.class, area, e -> e != mob)) {
            boolean rejected = (victim instanceof Player) || !SpacetimeTraits.isSpacetimeMob(victim);
            if (rejected) victim.hurt(src, dmg);
        }
    }
}
