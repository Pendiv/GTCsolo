package DIV.gtcsolo.l2.trait;

import DIV.gtcsolo.combat.arrow.ArrowBehaviors;
import DIV.gtcsolo.combat.arrow.SpecialArrow;
import DIV.gtcsolo.l2.trait.base.TypedMobTrait;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Arrow;

/**
 * 浮ついた矢 (Floating Arrow) — スケルトン専用。 player が自身より高所に居る時、 緩やかな浮遊を獲得し、
 * 放つ矢が浮遊エフェクト矢になる。
 *
 * <p>player が高所 (= self より +{@value #HEIGHT_MARGIN} 以上) の間のみ: self に浮遊 I を維持して追従し、
 * CD で player へ浮遊矢 ({@link ArrowBehaviors#SWALLOW} を弱性能 = 緩やか) を放つ。 lv 非依存。
 */
public class FloatingArrowTrait extends TypedMobTrait {

    private static final String CD_KEY = "gtcsolo.floating_arrow_cd";
    private static final double RADIUS = 32.0;
    private static final double HEIGHT_MARGIN = 2.0;
    private static final long COOLDOWN = 60;

    public FloatingArrowTrait(ChatFormatting style) {
        super(style);
    }

    @Override
    protected boolean isValidTarget(LivingEntity mob) {
        return mob instanceof AbstractSkeleton;
    }

    @Override
    protected void onValidTick(LivingEntity mob, int lv) {
        if (!(mob.level() instanceof ServerLevel sl)) return;
        Player target = sl.getNearestPlayer(mob, RADIUS);
        if (target == null) return;
        if (target.getY() <= mob.getY() + HEIGHT_MARGIN) return;  // player が高所の時のみ

        // self に緩やかな浮遊を維持 (= 高所の player へ追従)
        mob.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 40, 0, false, false));

        // CD で浮遊矢を放つ
        long now = sl.getGameTime();
        var pd = mob.getPersistentData();
        if (pd.contains(CD_KEY) && now - pd.getLong(CD_KEY) < COOLDOWN) return;
        pd.putLong(CD_KEY, now);
        Arrow arrow = SpecialArrow.aimedArrow(mob, target, 3.0f, 0.5f);
        SpecialArrow.put(arrow, ArrowBehaviors.SWALLOW, 2.0);  // 緩やか (= Levitation II 相当)
        sl.addFreshEntity(arrow);
    }
}
