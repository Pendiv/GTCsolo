package DIV.gtcsolo.l2.trait;

import DIV.gtcsolo.l2.SpacetimeTraits;
import DIV.gtcsolo.l2.trait.base.ISpacetimeTrait;
import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import net.minecraft.ChatFormatting;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;

/**
 * 時空の潮汐力 (Spacetime Tidal Force) — 周囲の時空タイプ mob にかかった debuff の継続時間を早く経過させる。
 * 1 秒ごとに余分に時間を減らし、 減らす量は level が上がるほど大きい。
 *
 * <p>範囲内の時空 mob (= player 除く) の HARMFUL effect を走査し、 通常の経過に加えて
 * {@link #EXTRA_PER_LEVEL} × level tick 分だけ残り時間を削る (= 0 以下なら除去)。
 */
public class SpacetimeTidalForceTrait extends MobTrait implements ISpacetimeTrait {

    private static final int INTERVAL = 20;
    private static final double RADIUS = 8.0;
    private static final int EXTRA_PER_LEVEL = 20;  // 追加で 1 秒/level 早める

    public SpacetimeTidalForceTrait(ChatFormatting style) {
        super(style);
    }

    @Override
    public void tick(LivingEntity mob, int level) {
        super.tick(mob, level);
        if (mob.level().isClientSide()) return;
        if (mob.tickCount % INTERVAL != 0) return;
        int extra = EXTRA_PER_LEVEL * level;
        if (extra <= 0) return;
        AABB area = mob.getBoundingBox().inflate(RADIUS);
        for (LivingEntity ally : mob.level().getEntitiesOfClass(LivingEntity.class, area, e -> e != mob)) {
            if (ally instanceof Player) continue;
            if (!SpacetimeTraits.isSpacetimeMob(ally)) continue;
            for (MobEffectInstance inst : new ArrayList<>(ally.getActiveEffects())) {
                if (inst.getEffect().getCategory() != MobEffectCategory.HARMFUL) continue;
                int newDur = inst.getDuration() - extra;
                ally.removeEffect(inst.getEffect());
                if (newDur > 0) {
                    ally.addEffect(new MobEffectInstance(inst.getEffect(), newDur, inst.getAmplifier(),
                            inst.isAmbient(), inst.isVisible(), inst.showIcon()));
                }
            }
        }
    }
}
