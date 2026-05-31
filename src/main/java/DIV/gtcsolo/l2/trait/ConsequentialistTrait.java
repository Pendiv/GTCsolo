package DIV.gtcsolo.l2.trait;

import DIV.gtcsolo.combat.arrow.ArrowBehaviors;
import DIV.gtcsolo.combat.arrow.SpecialArrow;
import DIV.gtcsolo.l2.trait.base.SkeletonArrowTrait;
import net.minecraft.ChatFormatting;
import net.minecraft.world.entity.projectile.Arrow;

/**
 * 結果論者 (Consequentialist) — スケルトン専用。 短いクールタイムで帰結の矢 (hitscan) を放つ。
 *
 * <p>帰結の矢は飛翔をキャンセルし発射方向へ即着弾する ({@link ArrowBehaviors#CONSEQUENCE})。
 * <p>CD = max(1 秒, (80 - 15n) tick) (= 仮値、 バランスは L2 の level/cost/weight に委ねる)。
 */
public class ConsequentialistTrait extends SkeletonArrowTrait {

    public ConsequentialistTrait(ChatFormatting style) {
        super(style);
    }

    @Override
    protected long cooldown(int lv) {
        return Math.max(20, 80 - 15L * lv);
    }

    @Override
    protected void tagArrow(Arrow arrow, int lv) {
        SpecialArrow.put(arrow, ArrowBehaviors.CONSEQUENCE);
    }
}
