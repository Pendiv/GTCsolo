package DIV.gtcsolo.l2.trait;

import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import net.minecraft.ChatFormatting;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

/**
 * [15] Gambler — 受けるダメージを 20% の確率で大ブレ抽選 (= 軽減 or 増幅)、 ランクで振れ幅増大。
 *
 * <p>1/5 (= 20%) で抽選発火、 当選時 50/50 で軽減 or 増幅。 rank r → 軽減 1/(r+1)・増幅 (r+1) 倍
 * (= lv1: 1/2 ⇔ 2x)。 残り 80% は素通し。
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
