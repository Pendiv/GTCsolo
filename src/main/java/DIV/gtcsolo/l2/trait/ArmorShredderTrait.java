package DIV.gtcsolo.l2.trait;

import DIV.gtcsolo.combat.arrow.ArrowBehaviors;
import DIV.gtcsolo.combat.arrow.SpecialArrow;
import DIV.gtcsolo.l2.trait.base.SkeletonArrowTrait;
import net.minecraft.ChatFormatting;
import net.minecraft.world.entity.projectile.Arrow;

/**
 * 鎧削り (Armor Shredder) — スケルトン専用。 撃滅の矢 (防御貫通) を放つ。
 *
 * <p>{@link ArrowBehaviors#ANNIHILATION} = post-armor ダメージに防具値 × 貫通率を足し戻す近似。
 * <p>Lv で CD 減少 + 貫通率上昇 (= 仮値、 バランスは L2 に委ねる)。
 * <p>[要確認] 仕様の「プレイヤー敵対初回にも放つ」 は未実装 (現状 CD のみ)。
 */
public class ArmorShredderTrait extends SkeletonArrowTrait {

    public ArmorShredderTrait(ChatFormatting style) {
        super(style);
    }

    @Override
    protected long cooldown(int lv) {
        return Math.max(100, 600 - 100L * lv);  // 30 秒基準、 -5 秒/lv、 下限 5 秒
    }

    @Override
    protected void tagArrow(Arrow arrow, int lv) {
        double pierce = Math.min(1.0, 0.5 + 0.25 * lv);  // lv1 75% → lv2+ 100%
        SpecialArrow.put(arrow, ArrowBehaviors.ANNIHILATION, pierce);
    }
}
