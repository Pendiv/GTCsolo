package DIV.gtcsolo.l2.trait;

import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import net.minecraft.ChatFormatting;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

/**
 * 腹が減っては戦ができぬ (Well-Fed Defense) — player から攻撃を受けるとき、
 * 満腹度が最大から減少しているなら、 その減少割合と同値受けるダメージを低減する。
 *
 * <p>仕様 (★ lv 無し、 純線形):
 * <ul>
 *   <li>attacker が Player なら foodLevel 取得</li>
 *   <li>削減割合 = (20 - foodLevel) / 20 (= foodLevel 20 で 0%、 10 で 50%、 0 で 100%)</li>
 *   <li>受けるダメージ = original × (1 - 削減割合) = original × (foodLevel / 20)</li>
 *   <li>foodLevel 0 で完全無効化 (= ダメ 0)</li>
 * </ul>
 */
public class WellFedDefenseTrait extends MobTrait {

    public WellFedDefenseTrait(ChatFormatting style) {
        super(style);
    }

    @Override
    public void onHurtByOthers(int level, LivingEntity entity, LivingHurtEvent event) {
        super.onHurtByOthers(level, entity, event);
        if (!(event.getSource().getEntity() instanceof Player p)) return;
        float foodLevel = p.getFoodData().getFoodLevel();
        float retention = Math.max(0f, Math.min(1f, foodLevel / 20.0f));
        event.setAmount(event.getAmount() * retention);
    }
}
