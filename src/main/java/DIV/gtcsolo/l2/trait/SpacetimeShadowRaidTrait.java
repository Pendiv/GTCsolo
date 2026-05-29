package DIV.gtcsolo.l2.trait;

import DIV.gtcsolo.l2.trait.base.ISpacetimeTrait;
import DIV.gtcsolo.registry.ModEffects;
import dev.xkmc.l2damagetracker.contents.attack.AttackCache;
import dev.xkmc.l2hostility.content.logic.TraitEffectCache;
import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import net.minecraft.ChatFormatting;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

/**
 * 時空の影襲 (Spacetime Shadow Raid) — player に与えたダメージ分だけ {@link ModEffects#CONCEALMENT 隠蔽}
 * を付与する。 隠蔽は最大 HP を同量だけ削り (= 削れた分が「隠された体力」)、 持続は 2 分。
 *
 * <p>隠蔽は MobEffect 由来でない回復で徐々に戻る (= ConcealmentEffect 側の挙動)。
 * 与ダメ 2 につき amplifier 1 段 (= 最大 HP -2/段) 加算する。 既存の隠蔽には積み増す。
 */
public class SpacetimeShadowRaidTrait extends MobTrait implements ISpacetimeTrait {

    private static final int DURATION = 2400;  // 2 分
    private static final int MAX_AMPLIFIER = 49; // 最大 HP -100 まで

    public SpacetimeShadowRaidTrait(ChatFormatting style) {
        super(style);
    }

    @Override
    public void onHurtTarget(int level, LivingEntity attacker, AttackCache cache, TraitEffectCache traitCache) {
        super.onHurtTarget(level, attacker, cache, traitCache);
        LivingHurtEvent e = cache.getLivingHurtEvent();
        if (e == null) return;
        if (!(e.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide()) return;
        float dmg = e.getAmount();
        if (dmg <= 0f) return;

        int add = Math.max(1, Math.round(dmg / 2f));  // 2 HP = 1 段
        MobEffectInstance cur = player.getEffect(ModEffects.CONCEALMENT.get());
        int curAmp = cur != null ? cur.getAmplifier() : -1;
        int newAmp = Math.min(curAmp + add, MAX_AMPLIFIER);
        player.addEffect(new MobEffectInstance(ModEffects.CONCEALMENT.get(), DURATION, newAmp, false, true));
    }
}
