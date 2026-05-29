package DIV.gtcsolo.l2.trait;

import DIV.gtcsolo.l2.trait.base.ISpacetimeTrait;
import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import net.minecraft.ChatFormatting;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * 時空の跳躍 (Spacetime Leap) — 周期的に player の近傍へ瞬間移動する。
 * player が近くにいるか、 こちらを認識しているかは問わない (= dimension 内の最寄り player を狙う)。
 *
 * <p>周期は 1200 tick 固定 (level 非依存)。 teleport は周囲に着地点を試行し、 有効な座標が
 * 見つかれば移動する。
 */
public class SpacetimeLeapTrait extends MobTrait implements ISpacetimeTrait {

    private static final int INTERVAL = 1200;           // 60 秒固定 (level 非依存)
    private static final double SPREAD = 5.0;            // player から ±5 ブロック

    public SpacetimeLeapTrait(ChatFormatting style) {
        super(style);
    }

    @Override
    public void tick(LivingEntity mob, int level) {
        super.tick(mob, level);
        if (mob.level().isClientSide()) return;
        if (mob.tickCount % INTERVAL != 0) return;
        Player target = mob.level().getNearestPlayer(mob, -1.0);  // -1 = 距離無制限
        if (target == null) return;
        for (int i = 0; i < 12; i++) {
            double x = target.getX() + (mob.getRandom().nextDouble() * 2 - 1) * SPREAD;
            double z = target.getZ() + (mob.getRandom().nextDouble() * 2 - 1) * SPREAD;
            double y = target.getY();
            if (mob.randomTeleport(x, y, z, true)) break;
        }
    }
}
