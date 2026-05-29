package DIV.gtcsolo.l2.trait;

import DIV.gtcsolo.l2.ModL2Traits;
import DIV.gtcsolo.l2.SpacetimeTraits;
import DIV.gtcsolo.l2.trait.base.ISpacetimeTrait;
import dev.xkmc.l2hostility.content.capability.mob.MobTraitCap;
import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import net.minecraft.ChatFormatting;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

/**
 * 時空の超越者 (Spacetime Transcendent) — 時空タイプを持たない mob を即殺する。 ただし 15% の確率で、
 * 殺さず対象に時空タイプの特性を付与する (= 見逃して同類化)。 自身は他 mob の AI ターゲットから除外される
 * (= {@code TargetingConditionsMixin})。
 *
 * <p>付与する特性は {@link ModL2Traits#randomGrantableSpacetime} のプールから抽選する。
 */
public class SpacetimeTranscendentTrait extends MobTrait implements ISpacetimeTrait {

    private static final int INTERVAL = 20;
    private static final double RADIUS = 12.0;
    private static final double CONVERT_CHANCE = 0.15;

    public SpacetimeTranscendentTrait(ChatFormatting style) {
        super(style);
    }

    @Override
    public void tick(LivingEntity mob, int level) {
        super.tick(mob, level);
        if (mob.level().isClientSide()) return;
        if (mob.tickCount % INTERVAL != 0) return;
        AABB area = mob.getBoundingBox().inflate(RADIUS);
        for (LivingEntity e : mob.level().getEntitiesOfClass(LivingEntity.class, area, x -> x != mob)) {
            if (e instanceof Player) continue;
            if (!(e instanceof Mob)) continue;
            if (SpacetimeTraits.isSpacetimeMob(e)) continue;  // 時空 mob は対象外
            if (mob.getRandom().nextDouble() < CONVERT_CHANCE && MobTraitCap.HOLDER.isProper(e)) {
                MobTrait granted = ModL2Traits.randomGrantableSpacetime(mob.getRandom());
                if (granted != null) {
                    MobTraitCap cap = MobTraitCap.HOLDER.get(e);
                    if (cap.traits.get(granted) == null) cap.setTrait(granted, Math.max(1, level));
                }
            } else {
                e.discard();  // 即殺 (= 消滅させる)
            }
        }
    }
}
