package DIV.gtcsolo.l2.trait;

import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * [28] Wizardry — 低確率 (= tick あたり) で最寄り player と座標スワップ。
 *
 * <p>確率 = n/180 per tick (= 分母 180÷n。 lv1 で約 9 秒に 1 回、 lv3 で約 3 秒に 1 回)。
 * 落下中の player を空中へ飛ばさないよう、 双方の足元が固体ブロックの時のみ発火。
 */
public class WizardryTrait extends MobTrait {

    private static final int BASE_DENOMINATOR = 180;  // 発動確率 = n/180 per tick (= 180÷n の分母)
    private static final double SEARCH_RADIUS = 16.0;

    public WizardryTrait(ChatFormatting style) {
        super(style);
    }

    @Override
    public void tick(LivingEntity mob, int level) {
        Level world = mob.level();
        if (world.isClientSide()) return;
        if (world.random.nextInt(Math.max(1, BASE_DENOMINATOR / level)) != 0) return;

        Player player = world.getNearestPlayer(mob, SEARCH_RADIUS);
        if (player == null) return;

        if (!isFeetSafe(world, mob) || !isFeetSafe(world, player)) return;

        Vec3 mobPos = mob.position();
        Vec3 plPos = player.position();
        mob.teleportTo(plPos.x, plPos.y, plPos.z);
        player.teleportTo(mobPos.x, mobPos.y, mobPos.z);
    }

    private static boolean isFeetSafe(Level world, LivingEntity e) {
        BlockPos below = e.blockPosition().below();
        return world.getBlockState(below).isSolid();
    }
}
