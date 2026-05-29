package DIV.gtcsolo.l2.trait;

import dev.xkmc.l2damagetracker.contents.attack.AttackCache;
import dev.xkmc.l2hostility.content.logic.TraitEffectCache;
import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import net.minecraft.ChatFormatting;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

/**
 * 腹満ちて戦時 (Well-Fed Strike) — player の体力割合が多いほどより攻撃力増加。
 *
 * <p>仕様:
 * <ul>
 *   <li>onHurtTarget で target が Player なら health / maxHealth で health 割合計算</li>
 *   <li>M = player HP 割合 (0-100)、 倍率 = 1 + (M + 20N) × 0.4%</li>
 *   <li>lv1 + 満タン (M=100) で +48%、 lv5 + 満タンで +80%</li>
 * </ul>
 */
public class WellFedStrikeTrait extends MobTrait {

    public WellFedStrikeTrait(ChatFormatting style) {
        super(style);
    }

    @Override
    public void onHurtTarget(int level, LivingEntity attacker, AttackCache cache, TraitEffectCache traitCache) {
        super.onHurtTarget(level, attacker, cache, traitCache);
        LivingHurtEvent event = cache.getLivingHurtEvent();
        if (event == null) return;
        if (!(traitCache.target instanceof Player p)) return;
        float max = p.getMaxHealth();
        if (max <= 0) return;
        float ratio = Math.max(0f, Math.min(1f, p.getHealth() / max));
        double m = ratio * 100.0;                        // HP 割合 (百分率)
        double mult = 1.0 + (m + 20.0 * level) * 0.004;  // (M + 20N) × 0.4% 増加
        event.setAmount(event.getAmount() * (float) mult);
    }
}
