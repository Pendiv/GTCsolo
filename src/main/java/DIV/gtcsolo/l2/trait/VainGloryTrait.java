package DIV.gtcsolo.l2.trait;

import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import net.minecraft.ChatFormatting;
import net.minecraft.world.entity.LivingEntity;

/**
 * 虚飾 (Vain Glory) — 特殊体力に移行する。 元の体力の 200% を保持し全ての受けるダメージ/
 * 回復は特殊体力 (= vanilla absorption) に反映される。 特殊体力が 0 になると死亡する。
 *
 * <p>仕様 (= vanilla absorption 利用、 attribute 不要):
 * <ul>
 *   <li>postInit で setAbsorptionAmount(maxHealth × 2) で初期 phantom HP 設定</li>
 *   <li>本体 HP は tick で常に maxHealth 固定 (= absorption 切れて流れ込んだら即 setHealth(0))</li>
 *   <li>結果: 全ダメは vanilla absorption-first ルールで absorption から減算、 0 で死亡</li>
 *   <li>回復は本体 HP に行くが maxHealth に capped、 absorption 自然増分は無いので effectively 無効</li>
 * </ul>
 *
 * <p>注: lv に関係なく一律 200%。 vanilla 1.20.1 には MAX_ABSORPTION attribute が無い (=
 * absorption は entity field 直接管理)、 setAbsorptionAmount で自由値 set 可能なので
 * cap 制御不要。
 */
public class VainGloryTrait extends MobTrait {

    public VainGloryTrait(ChatFormatting style) {
        super(style);
    }

    @Override
    public void postInit(LivingEntity mob, int lv) {
        super.postInit(mob, lv);
        if (mob.level().isClientSide()) return;
        mob.setAbsorptionAmount((float) (mob.getMaxHealth() * 2.0));
    }

    @Override
    public void tick(LivingEntity mob, int level) {
        super.tick(mob, level);
        if (mob.level().isClientSide()) return;
        if (mob.getAbsorptionAmount() <= 0f) {
            mob.setHealth(0f);
            return;
        }
        if (mob.getHealth() < mob.getMaxHealth()) {
            mob.setHealth(0f);
        }
    }
}
