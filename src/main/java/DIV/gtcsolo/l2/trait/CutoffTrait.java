package DIV.gtcsolo.l2.trait;

import com.mojang.logging.LogUtils;
import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import net.minecraft.ChatFormatting;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import org.slf4j.Logger;

/**
 * [22] Cutoff (見切り) — 最大体力の 1% に満たないダメージを無効化。
 * <p>耐久型: 雑魚ダメをまるごと足切りする。 大きな一撃でしか削れない。
 */
public class CutoffTrait extends MobTrait {
    private static final Logger LOGGER = LogUtils.getLogger();

    /** 足切り閾値: max HP に対する比率 (= これ未満のダメージは無効化) */
    private static final float CUTOFF_RATIO = 0.01f;

    public CutoffTrait(ChatFormatting style) {
        super(style);
    }

    @Override
    public void onHurtByOthers(int level, LivingEntity entity, LivingHurtEvent event) {
        float maxHp = entity.getMaxHealth();
        if (maxHp <= 0) return;
        float threshold = maxHp * CUTOFF_RATIO;
        if (event.getAmount() < threshold) {
            event.setCanceled(true);
            LOGGER.info("[Cutoff] cancel chip damage on {}: {} < threshold {} ({}% of {})",
                    entity, event.getAmount(), threshold, CUTOFF_RATIO * 100, maxHp);
        }
    }
}
