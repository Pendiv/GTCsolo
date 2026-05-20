package DIV.gtcsolo.l2.trait;

import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import net.minecraft.ChatFormatting;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

/**
 * [58] Ectotherm (変温動物) — 熱ダメージ・寒さダメージを無効化する。
 *
 * <p>対象 tag: vanilla {@link DamageTypeTags#IS_FIRE} (lava/in_fire/on_fire/hot_floor/fireball)、
 * {@link DamageTypeTags#IS_FREEZING} (freeze)。
 * L2 系には専用寒冷 tag が無いため vanilla tag のみで判定。
 */
public class EctothermTrait extends MobTrait {

    public EctothermTrait(ChatFormatting style) {
        super(style);
    }

    @Override
    public void onHurtByOthers(int level, LivingEntity entity, LivingHurtEvent event) {
        DamageSource src = event.getSource();
        if (src.is(DamageTypeTags.IS_FIRE) || src.is(DamageTypeTags.IS_FREEZING)) {
            event.setCanceled(true);
        }
    }
}
