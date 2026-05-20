package DIV.gtcsolo.l2.trait;

import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import net.minecraft.ChatFormatting;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

/**
 * [51] Divine Might (神威) — 失楽園の上位特性、 同居拒否。
 *
 * <p>受けるダメージを「素手相当」 (= 1.0) に cap する。 武器攻撃力・エンチャント・ポーションによる
 * 増加分を全て無効化する高 tier 特性。
 *
 * <p>失楽園との排他は datapack 配布側で実現せず、 効果的には divine_might が常に優先される
 * (= setAmount(min(1.0, current)) で paradise_lost の効果も巻き込んで cap される)。
 */
public class DivineMightTrait extends MobTrait {

    private static final float BASE_DAMAGE_CAP = 1.0f;

    public DivineMightTrait(ChatFormatting style) {
        super(style);
    }

    @Override
    public void onHurtByOthers(int level, LivingEntity entity, LivingHurtEvent event) {
        if (!(event.getSource().getEntity() instanceof Player)) return;
        if (event.getAmount() > BASE_DAMAGE_CAP) {
            event.setAmount(BASE_DAMAGE_CAP);
        }
    }
}
