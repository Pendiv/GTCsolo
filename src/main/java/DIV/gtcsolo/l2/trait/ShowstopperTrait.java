package DIV.gtcsolo.l2.trait;

import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import net.minecraft.ChatFormatting;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

/**
 * 超目立ちたがり屋 (Showstopper) — 常に燃え盛り発光する。 火炎耐性有。 {@link AttentionSeekerTrait} の上位。
 *
 * <p>仕様:
 * <ul>
 *   <li>postInit で setGlowingTag(true) = 常時発光</li>
 *   <li>tick で setRemainingFireTicks 補充 = 常時燃える演出</li>
 *   <li>onHurtByOthers で IS_FIRE ダメージ cancel = 火炎耐性 (= 自分の炎 + 外部の炎 両方無効)</li>
 * </ul>
 *
 * <p>注: 自身が常時燃えるが火炎耐性で焼死しない。 fire tick damage が onHurtByOthers で
 * 拾えない環境では別途 fireImmune 化が要るが、 まず標準 hook で対処。
 */
public class ShowstopperTrait extends MobTrait {

    public ShowstopperTrait(ChatFormatting style) {
        super(style);
    }

    @Override
    public void postInit(LivingEntity mob, int lv) {
        super.postInit(mob, lv);
        if (mob.level().isClientSide()) return;
        mob.setGlowingTag(true);
    }

    @Override
    public void tick(LivingEntity mob, int level) {
        if (mob.level().isClientSide()) return;
        if (mob.getRemainingFireTicks() < 20) {
            mob.setRemainingFireTicks(40);
        }
    }

    @Override
    public void onHurtByOthers(int level, LivingEntity entity, LivingHurtEvent event) {
        if (event.getSource().is(DamageTypeTags.IS_FIRE)) {
            event.setCanceled(true);
        }
    }
}
