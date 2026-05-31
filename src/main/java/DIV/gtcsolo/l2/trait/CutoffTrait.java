package DIV.gtcsolo.l2.trait;

import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import net.minecraft.ChatFormatting;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

/**
 * [22] Cutoff (見切り) — 最大体力の 1% に満たないダメージを無効化。
 * <p>耐久型: 雑魚ダメをまるごと足切りする。 大きな一撃でしか削れない。
 */
public class CutoffTrait extends MobTrait {

    /** 足切り閾値: max HP に対する比率 (= これ未満のダメージは無効化) */
    private static final float CUTOFF_RATIO = 0.01f;

    public CutoffTrait(ChatFormatting style) {
        super(style);
    }

    @Override
    public void onHurtByOthers(int level, LivingEntity entity, LivingHurtEvent event) {
        float maxHp = entity.getMaxHealth();
        if (maxHp <= 0) return;
        if (event.getAmount() < maxHp * CUTOFF_RATIO) {
            event.setCanceled(true);
        }
    }
}
