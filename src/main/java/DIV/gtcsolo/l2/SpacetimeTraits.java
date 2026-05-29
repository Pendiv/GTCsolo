package DIV.gtcsolo.l2;

import DIV.gtcsolo.l2.trait.base.ISpacetimeTrait;
import dev.xkmc.l2hostility.content.capability.mob.MobTraitCap;
import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import net.minecraft.world.entity.LivingEntity;

/**
 * 時空タイプ群の判定 helper。
 *
 * <p>「時空タイプの特性を持つ/持たない mob」 を参照する trait 群 (= 排斥 / 共鳴 / 潮汐力 /
 * 覇者 / 超越者 等) はここを経由して統一判定する。 判定基準は
 * {@link ISpacetimeTrait} を実装した trait を 1 つでも持つか。
 */
public final class SpacetimeTraits {

    private SpacetimeTraits() {}

    /** mob が時空タイプの特性を 1 つ以上持つか (= 時空の参入含む) */
    public static boolean isSpacetimeMob(LivingEntity mob) {
        if (!MobTraitCap.HOLDER.isProper(mob)) return false;
        MobTraitCap cap = MobTraitCap.HOLDER.get(mob);
        for (MobTrait t : cap.traits.keySet()) {
            if (t instanceof ISpacetimeTrait) return true;
        }
        return false;
    }

    /**
     * 他 mob の AI ターゲットから除外すべきか (= 潜航 / 超越者 保持)。
     * {@code TargetingConditionsMixin} から高頻度で呼ばれる。
     */
    public static boolean isAiTargetExcluded(LivingEntity mob) {
        if (!MobTraitCap.HOLDER.isProper(mob)) return false;
        MobTraitCap cap = MobTraitCap.HOLDER.get(mob);
        return cap.getTraitLevel(ModL2Traits.SPACETIME_DIVE.get()) > 0
                || cap.getTraitLevel(ModL2Traits.SPACETIME_TRANSCENDENT.get()) > 0;
    }
}
