package DIV.gtcsolo.l2.trait;

import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import net.minecraft.ChatFormatting;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

/**
 * 金剛夜叉 (Kongō Yasha) — 受けるダメージを 60% 軽減する。 代わりに player から攻撃を受ける際、
 * 最大 HP の 0.38% を軽減不可ダメージとして受ける。
 *
 * <p>= 超耐久だが、 player の攻撃には必ず最大 HP 0.38% の確定削りが乗る (= 無限タンク化を防ぐ)。 level 非依存。
 */
public class KongoYashaTrait extends MobTrait {

    private static final float REDUCTION = 0.6f;     // 60% 軽減
    private static final float CHIP_PCT = 0.0038f;   // 最大 HP 0.38% の軽減不可 chip

    public KongoYashaTrait(ChatFormatting style) {
        super(style);
    }

    @Override
    public void onHurtByOthers(int level, LivingEntity entity, LivingHurtEvent event) {
        // /kill 等の貫通ダメージはそのまま通す
        if (event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return;
        float reduced = event.getAmount() * (1.0f - REDUCTION);  // ×0.4
        if (event.getSource().getEntity() instanceof Player) {
            reduced += entity.getMaxHealth() * CHIP_PCT;  // 軽減不可 chip を加算
        }
        event.setAmount(reduced);
    }
}
