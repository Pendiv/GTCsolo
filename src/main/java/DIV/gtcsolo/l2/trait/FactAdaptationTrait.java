package DIV.gtcsolo.l2.trait;

import dev.xkmc.l2damagetracker.contents.attack.AttackCache;
import dev.xkmc.l2hostility.content.logic.TraitEffectCache;
import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import net.minecraft.ChatFormatting;
import net.minecraft.world.entity.LivingEntity;

/**
 * [02] Fact Adaptation — 攻撃時に対象の Absorption (黄色体力) を全て剥がす。
 */
public class FactAdaptationTrait extends MobTrait {

    public FactAdaptationTrait(ChatFormatting style) {
        super(style);
    }

    @Override
    public void onHurtTarget(int level, LivingEntity attacker, AttackCache cache, TraitEffectCache traitCache) {
        super.onHurtTarget(level, attacker, cache, traitCache);
        traitCache.target.setAbsorptionAmount(0);
    }
}
