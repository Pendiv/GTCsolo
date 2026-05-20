package DIV.gtcsolo.l2.trait;

import com.mojang.logging.LogUtils;
import dev.xkmc.l2damagetracker.contents.attack.AttackCache;
import dev.xkmc.l2hostility.content.logic.TraitEffectCache;
import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import net.minecraft.ChatFormatting;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import org.slf4j.Logger;

/**
 * [18] Anger — 体力が低いほど与ダメージが上昇。
 *
 * <p>倍率 = 1 + (1 - hp_ratio) * 0.5 * level
 *   (例: lv1 で hp 0% → 1.5x、 lv3 で hp 0% → 2.5x)
 */
public class AngerTrait extends MobTrait {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final double BONUS_PER_LEVEL = 0.5;

    public AngerTrait(ChatFormatting style) {
        super(style);
    }

    @Override
    public void onHurtTarget(int level, LivingEntity attacker, AttackCache cache, TraitEffectCache traitCache) {
        super.onHurtTarget(level, attacker, cache, traitCache);
        LivingHurtEvent event = cache.getLivingHurtEvent();
        if (event == null) return;
        float maxHp = attacker.getMaxHealth();
        if (maxHp <= 0) return;
        double hpRatio = attacker.getHealth() / maxHp;
        double mult = 1.0 + (1.0 - hpRatio) * BONUS_PER_LEVEL * level;
        float before = event.getAmount();
        event.setAmount(before * (float) mult);
        LOGGER.info("[Anger:{}] hp_ratio={}, mult={}, dmg {} -> {}",
                attacker, hpRatio, mult, before, event.getAmount());
    }
}
