package DIV.gtcsolo.l2.trait;

import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import net.minecraft.ChatFormatting;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingAttackEvent;

/**
 * [11] Innocence Battle — 128 ブロック超の遠距離攻撃 + 非 player 由来ダメージを完全拒否。
 *
 * <p>遠距離からの一方的な狩り + 環境ダメージによる消化を禁止、 正面交戦を強制する。
 */
public class InnocenceBattleTrait extends MobTrait {

    private static final double MAX_RANGE_SQ = 128.0 * 128.0;

    public InnocenceBattleTrait(ChatFormatting style) {
        super(style);
    }

    @Override
    public void onAttackedByOthers(int level, LivingEntity entity, LivingAttackEvent event) {
        var srcEntity = event.getSource().getEntity();
        // player 以外からのダメージ = 全部拒否 (= 環境ダメージ・他 mob 等)
        if (!(srcEntity instanceof Player player)) {
            event.setCanceled(true);
            return;
        }
        // 128 ブロック超の遠距離 = 拒否
        if (player.distanceToSqr(entity) > MAX_RANGE_SQ) {
            event.setCanceled(true);
        }
    }
}
