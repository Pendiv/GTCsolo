package DIV.gtcsolo.l2.trait;

import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import net.minecraft.ChatFormatting;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

/**
 * [23] Magical Creatures — 受けた魔法ダメージをそのまま回復に転換。
 *
 * <p>魔法依存の攻撃を逆用、 魔法では倒せない相手にする。
 */
public class MagicalCreaturesTrait extends MobTrait {

    public MagicalCreaturesTrait(ChatFormatting style) {
        super(style);
    }

    @Override
    public void onHurtByOthers(int level, LivingEntity entity, LivingHurtEvent event) {
        if (!event.getSource().is(DamageTypeTags.WITCH_RESISTANT_TO)) return;
        float heal = event.getAmount();
        event.setCanceled(true);
        entity.heal(heal);  // heal() は内部で ForgeEventFactory.onLivingHeal を尊重し maxHP で cap
    }
}
