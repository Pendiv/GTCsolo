package DIV.gtcsolo.l2.trait;

import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import net.minecraft.ChatFormatting;
import net.minecraft.world.entity.LivingEntity;

/**
 * [49] Prepared (準備万端) — 出現時に「最大 HP × 割合」 の Absorption (黄色体力) を獲得する。
 *
 * <p>割合 = 20% × level (= lv1 で +20%、 lv5 で +100% の黄色体力)。
 * <p>Fact Adaptation (= 緩衝体力剥がし) と対をなすテーマ。
 */
public class PreparedTrait extends MobTrait {

    private static final float ABSORPTION_RATIO_PER_LEVEL = 0.2f;

    public PreparedTrait(ChatFormatting style) {
        super(style);
    }

    @Override
    public void postInit(LivingEntity mob, int lv) {
        super.postInit(mob, lv);
        if (mob.level().isClientSide()) return;
        float amount = mob.getMaxHealth() * ABSORPTION_RATIO_PER_LEVEL * lv;
        mob.setAbsorptionAmount(amount);
    }
}
