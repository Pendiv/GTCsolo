package DIV.gtcsolo.l2.trait;

import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import net.minecraft.ChatFormatting;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

/**
 * [54] Just Parry (ジャストパリイ) — プレイヤー攻撃を稀に完全無効化する。 確率はレベルで増加。
 *
 * <p>確率 = (30 + 7n)% (= lv1 で 37%、 lv3 で 51%)。
 * <p>Gambler が「軽減 / 増幅 / 通常」 のテーブルなのに対し、 こちらは「無効化 / 通常」 の二択。
 */
public class JustParryTrait extends MobTrait {

    private static final double BASE_CHANCE = 0.30;
    private static final double CHANCE_PER_LEVEL = 0.07;  // パリィ率 = (30 + 7n)%

    public JustParryTrait(ChatFormatting style) {
        super(style);
    }

    @Override
    public void onHurtByOthers(int level, LivingEntity entity, LivingHurtEvent event) {
        if (!(event.getSource().getEntity() instanceof Player)) return;
        double chance = BASE_CHANCE + CHANCE_PER_LEVEL * level;
        if (entity.getRandom().nextDouble() < chance) {
            event.setCanceled(true);
        }
    }
}
