package DIV.gtcsolo.l2.trait;

import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import net.minecraft.ChatFormatting;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

/**
 * [48] Iron Legs (足腰を鍛える) — 落下ダメージを無効化する。 レベル無し。
 */
public class IronLegsTrait extends MobTrait {

    public IronLegsTrait(ChatFormatting style) {
        super(style);
    }

    @Override
    public void onHurtByOthers(int level, LivingEntity entity, LivingHurtEvent event) {
        if (event.getSource().is(DamageTypes.FALL)) {
            event.setCanceled(true);
        }
    }
}
