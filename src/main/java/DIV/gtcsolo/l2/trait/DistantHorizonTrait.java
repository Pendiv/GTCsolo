package DIV.gtcsolo.l2.trait;

import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import net.minecraft.ChatFormatting;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

/**
 * 悠遠の地平線 (Distant Horizon) — player から攻撃を受けるとき、 player の体力が最大から
 * 減少しているなら、 その減少割合と同値受けるダメージを低減する。
 *
 * <p>仕様 (★ lv 無し、 純線形、 {@link WellFedDefenseTrait} と対称構造):
 * <ul>
 *   <li>attacker が Player なら health / maxHealth で retention 計算</li>
 *   <li>受けるダメージ = original × (health / maxHealth)</li>
 *   <li>player 体力 0 で完全無効化 (= ダメ 0)、 満タンで原ダメ通り</li>
 * </ul>
 */
public class DistantHorizonTrait extends MobTrait {

    public DistantHorizonTrait(ChatFormatting style) {
        super(style);
    }

    @Override
    public void onHurtByOthers(int level, LivingEntity entity, LivingHurtEvent event) {
        super.onHurtByOthers(level, entity, event);
        if (!(event.getSource().getEntity() instanceof Player p)) return;
        float max = p.getMaxHealth();
        if (max <= 0) return;
        float retention = Math.max(0f, Math.min(1f, p.getHealth() / max));
        event.setAmount(event.getAmount() * retention);
    }
}
