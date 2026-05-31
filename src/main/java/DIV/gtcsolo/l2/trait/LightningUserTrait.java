package DIV.gtcsolo.l2.trait;

import DIV.gtcsolo.combat.arrow.ArrowBehaviors;
import DIV.gtcsolo.combat.arrow.SpecialArrow;
import DIV.gtcsolo.l2.trait.base.SkeletonArrowTrait;
import net.minecraft.ChatFormatting;
import net.minecraft.world.entity.projectile.Arrow;

/**
 * 雷使い (Lightning User) — スケルトン専用。 雷矢を放つ。
 *
 * <p>雷矢 = 着弾座標に雷を落とし、 対象の攻撃力を参照した追加ダメージ ({@link ArrowBehaviors#LIGHTNING})。
 * <p>CD 固定 20 秒。 性能 = 50% (lv1) から +20%/lv (lv5 で 130%)。 max_rank 5。
 */
public class LightningUserTrait extends SkeletonArrowTrait {

    public LightningUserTrait(ChatFormatting style) {
        super(style);
    }

    @Override
    protected long cooldown(int lv) {
        return 400;  // 固定 20 秒
    }

    @Override
    protected void tagArrow(Arrow arrow, int lv) {
        SpecialArrow.put(arrow, ArrowBehaviors.LIGHTNING, 0.3 + 0.2 * lv);  // lv1 50% → lv5 130%
    }
}
