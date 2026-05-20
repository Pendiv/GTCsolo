package DIV.gtcsolo.l2.trait;

import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import net.minecraft.ChatFormatting;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

/**
 * [15] Gambler — 受けるダメージに 20% で大ブレ抽選 (= 軽減 or 増幅)、 ランクで振れ幅増大。
 *
 * <p>確率テーブル (max_rank=3 想定):
 * <ul>
 *   <li>80% 通常 (= 1.0x)</li>
 *   <li>10% 軽減 (= 1/2 → 1/3 → 1/4)</li>
 *   <li>10% 増幅 (= 2x → 3x → 4x)</li>
 * </ul>
 */
public class GamblerTrait extends MobTrait {

    private static final int TRIGGER_DENOM = 5;    // 1/5 = 20%
    /** lv1=2, lv2=3, lv3=4 (= 軽減側分母 / 増幅側倍率) */
    private static final int[] LEVEL_FACTOR = {0, 2, 3, 4, 5, 6};

    public GamblerTrait(ChatFormatting style) {
        super(style);
    }

    @Override
    public void onHurtByOthers(int level, LivingEntity entity, LivingHurtEvent event) {
        if (entity.level().isClientSide()) return;
        int idx = Math.min(level, LEVEL_FACTOR.length - 1);
        int factor = LEVEL_FACTOR[idx];
        if (factor < 2) return;

        var rng = entity.getRandom();
        if (rng.nextInt(TRIGGER_DENOM) != 0) return; // 80% は通常スルー

        boolean amplify = rng.nextBoolean(); // 50/50 で軽減 or 増幅
        float mult = amplify ? (float) factor : (1f / factor);
        event.setAmount(event.getAmount() * mult);
    }
}
