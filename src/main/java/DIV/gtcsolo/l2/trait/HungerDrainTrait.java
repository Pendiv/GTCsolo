package DIV.gtcsolo.l2.trait;

import dev.xkmc.l2damagetracker.contents.attack.AttackCache;
import dev.xkmc.l2hostility.content.logic.TraitEffectCache;
import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import net.minecraft.ChatFormatting;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * 腹が減って戦ができぬ (Hunger Drain) — 攻撃すると対象 (= player) の満腹度を減少させる。
 *
 * <p>仕様:
 * <ul>
 *   <li>onHurtTarget で target が Player なら addExhaustion で満腹度減算</li>
 *   <li>exhaustion 量 = {@link #EXHAUSTION_PER_LEVEL} × lv = 0.5n 食料/攻撃</li>
 *   <li>vanilla exhaustion 4.0 = 食料 1 消費なので、 2.0 × n で 0.5n 食料消費</li>
 * </ul>
 */
public class HungerDrainTrait extends MobTrait {

    private static final float EXHAUSTION_PER_LEVEL = 2.0f;  // 0.5n 食料消費相当 (0.5 × 4.0 exhaustion/食料)

    public HungerDrainTrait(ChatFormatting style) {
        super(style);
    }

    @Override
    public void onHurtTarget(int level, LivingEntity attacker, AttackCache cache, TraitEffectCache traitCache) {
        super.onHurtTarget(level, attacker, cache, traitCache);
        if (!(traitCache.target instanceof Player p)) return;
        if (p.level().isClientSide()) return;
        p.getFoodData().addExhaustion(EXHAUSTION_PER_LEVEL * level);
    }
}
