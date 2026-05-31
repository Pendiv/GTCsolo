package DIV.gtcsolo.l2.trait;

import DIV.gtcsolo.l2.trait.base.ISpacetimeTrait;
import DIV.gtcsolo.l2.util.L2TraitAttributes;
import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import net.minecraft.ChatFormatting;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;

import java.util.UUID;

/**
 * 時空の混乱 (Spacetime Confusion) — 周期的に、 周囲の mob と player それぞれの攻撃力を
 * 50%〜200% の範囲でランダムに変動させる。 変動は 10 秒持続し、 10 秒ごとに振り直される。
 *
 * <p>各対象に固定 UUID の MULTIPLY_TOTAL modifier を付け替える (= 振り直し)。 transient なので
 * reload で消える。 範囲外へ出た対象は最後の変動を保持する (= 混沌的挙動として許容)。
 */
public class SpacetimeConfusionTrait extends MobTrait implements ISpacetimeTrait {

    private static final UUID MOD_CONFUSION = UUID.fromString("3c8d2f17-9a4b-4e6c-bd25-7f1a8e3c9d46");
    private static final int INTERVAL = 200;  // 10 秒ごとに振り直し
    private static final double RADIUS = 12.0;
    private static final double MIN_FACTOR = 0.5;
    private static final double MAX_FACTOR = 2.0;

    public SpacetimeConfusionTrait(ChatFormatting style) {
        super(style);
    }

    @Override
    public void tick(LivingEntity mob, int level) {
        super.tick(mob, level);
        if (mob.level().isClientSide()) return;
        if (mob.tickCount % INTERVAL != 0) return;
        AABB area = mob.getBoundingBox().inflate(RADIUS);
        for (LivingEntity t : mob.level().getEntitiesOfClass(LivingEntity.class, area, e -> e != mob)) {
            double factor = MIN_FACTOR + t.getRandom().nextDouble() * (MAX_FACTOR - MIN_FACTOR);
            L2TraitAttributes.setTransient(t, Attributes.ATTACK_DAMAGE, MOD_CONFUSION, "gtcsolo.spacetime_confusion",
                    factor - 1.0, AttributeModifier.Operation.MULTIPLY_TOTAL);
        }
    }
}
