package DIV.gtcsolo.l2.trait;

import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import net.minecraft.ChatFormatting;
import net.minecraft.world.entity.LivingEntity;

/**
 * [61] Attention Seeker (目立ちたがり屋) — 発光する。 それだけ。
 *
 * <p>仕様メモ: 「たまにカラフルな奴がいる」 (= scoreboard team color 経由で個体別 glow 色) は
 * 実装保留 (Phase 2 即実装不可、 future enhancement)。 現状は monochrome glow のみ。
 */
public class AttentionSeekerTrait extends MobTrait {

    public AttentionSeekerTrait(ChatFormatting style) {
        super(style);
    }

    @Override
    public void postInit(LivingEntity mob, int lv) {
        super.postInit(mob, lv);
        if (mob.level().isClientSide()) return;
        mob.setGlowingTag(true);
    }
}
