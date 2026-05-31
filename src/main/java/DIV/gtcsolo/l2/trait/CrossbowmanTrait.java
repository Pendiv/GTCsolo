package DIV.gtcsolo.l2.trait;

import DIV.gtcsolo.combat.arrow.ArrowBehaviors;
import DIV.gtcsolo.combat.arrow.SpecialArrow;
import DIV.gtcsolo.l2.trait.base.SkeletonArrowTrait;
import net.minecraft.ChatFormatting;
import net.minecraft.world.entity.projectile.Arrow;

/**
 * 弩使い (Crossbowman) — スケルトン専用。 反発矢 ({@link ArrowBehaviors#CROSSBOW}) を放つ。
 *
 * <p>発射速度は大幅に低下 (= 長 CD) だが、 弾速が速く、 KB 耐性を部分貫通して大きく吹き飛ばす。
 * 吹き飛ばし後は対象に一定時間「弩耐性」 が付くため連続拘束はされない (= behavior 側で処理)。
 * <p>Lv で CD 短縮 + KB 威力上昇 (= 仮値)。
 */
public class CrossbowmanTrait extends SkeletonArrowTrait {

    public CrossbowmanTrait(ChatFormatting style) {
        super(style);
    }

    @Override
    protected long cooldown(int lv) {
        return Math.max(100, 300 - 30L * lv);  // 発射速度大幅低下 = 長 CD
    }

    @Override
    protected float arrowSpeed() {
        return 4.0f;  // 弾速速い
    }

    @Override
    protected void tagArrow(Arrow arrow, int lv) {
        SpecialArrow.put(arrow, ArrowBehaviors.CROSSBOW, 5.0 + lv);  // KB 威力 (init 5 + lv)
    }
}
