package DIV.gtcsolo.l2.trait;

import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import net.minecraft.ChatFormatting;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

/**
 * [04] Rebirth (再起治癒) — プレイヤーから受けたダメージの一定割合を自己回復。
 *
 * <p>回復量 = 10% × level × 被ダメージ (= lv1 で 10%、 lv5 で 50%)
 * <p>プレイヤー由来でないダメージ (= 環境、 他 mob) は対象外。
 */
public class RebirthTrait extends MobTrait {

    private static final double HEAL_RATIO_PER_LEVEL = 0.12;  // 被ダメの 12n% を回復

    public RebirthTrait(ChatFormatting style) {
        super(style);
    }

    @Override
    public void onHurtByOthers(int level, LivingEntity entity, LivingHurtEvent event) {
        // プレイヤー由来のダメージのみ対象
        if (!(event.getSource().getEntity() instanceof Player)) return;
        float taken = event.getAmount();
        if (taken <= 0) return;
        float heal = (float) (taken * HEAL_RATIO_PER_LEVEL * level);
        float allowed = ForgeEventFactory.onLivingHeal(entity, heal);
        if (allowed > 0) {
            entity.setHealth(Math.min(entity.getMaxHealth(), entity.getHealth() + allowed));
        }
    }
}
