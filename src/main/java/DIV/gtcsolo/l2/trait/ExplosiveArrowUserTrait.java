package DIV.gtcsolo.l2.trait;

import DIV.gtcsolo.combat.arrow.ArrowBehaviors;
import DIV.gtcsolo.combat.arrow.SpecialArrow;
import DIV.gtcsolo.l2.trait.base.SkeletonArrowTrait;
import net.minecraft.ChatFormatting;
import net.minecraft.world.entity.projectile.Arrow;

/**
 * 爆裂矢使い (Explosive Arrow User) — スケルトン専用。 爆裂矢を放つ (= プレイヤー or 壁で爆発)。 CD 長め。
 *
 * <p>{@link ArrowBehaviors#EXPLOSIVE} = 着弾点で爆発 (ブロック非破壊)。 性能 = 爆発威力。
 * <p>Lv で威力上昇 (= 仮値)。 CD は長め。 バランスは L2 に委ねる。
 */
public class ExplosiveArrowUserTrait extends SkeletonArrowTrait {

    public ExplosiveArrowUserTrait(ChatFormatting style) {
        super(style);
    }

    @Override
    protected long cooldown(int lv) {
        return Math.max(200, 600 - 80L * lv);  // CD 長め: 30 秒基準、 -4 秒/lv、 下限 10 秒
    }

    @Override
    protected void tagArrow(Arrow arrow, int lv) {
        SpecialArrow.put(arrow, ArrowBehaviors.EXPLOSIVE, 4.0 + lv);  // 威力 ≈ 初期値 5 (lv1) から上昇
    }
}
