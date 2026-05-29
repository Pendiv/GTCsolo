package DIV.gtcsolo.l2.trait;

import com.mojang.logging.LogUtils;
import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import org.slf4j.Logger;

/**
 * [25] Walking Abyss — 奈落落下を検知 → 周囲の地表を探して TP。
 *
 * <p>事前 safe 座標記録は廃止、 落下時に world.getHeight() ベースで近傍の最高地表 y を探索。
 * 半径 = {@link #SEARCH_RADIUS} 固定 (= level 非依存)、 螺旋状で最初に見つかった (= 最も近い) 固体ブロックの上に着地。
 */
public class WalkingAbyssTrait extends MobTrait {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final int SEARCH_RADIUS = 250;  // 固定半径 (level 非依存)

    public WalkingAbyssTrait(ChatFormatting style) {
        super(style);
    }

    @Override
    public void tick(LivingEntity mob, int level) {
        Level world = mob.level();
        if (world.isClientSide()) return;
        // 奈落判定: y が minBuildHeight 未満
        if (mob.getY() >= world.getMinBuildHeight()) return;

        int radius = SEARCH_RADIUS;
        BlockPos safe = findNearbyLand(world, mob.blockPosition(), radius);
        if (safe == null) {
            LOGGER.info("[WalkingAbyss] {} fell into abyss but no land found within {}", mob, radius);
            return;
        }
        mob.teleportTo(safe.getX() + 0.5, safe.getY(), safe.getZ() + 0.5);
        mob.setDeltaMovement(0, 0, 0);
        LOGGER.info("[WalkingAbyss] rescued {} to land at {}", mob, safe);
    }

    /** 螺旋状に半径 radius まで探索、 各 (x,z) の WORLD_SURFACE 最上面を取得して固体なら return。 */
    private static BlockPos findNearbyLand(Level world, BlockPos origin, int radius) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int r = 0; r <= radius; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    // 内側 (= 既探索) スキップ
                    if (Math.max(Math.abs(dx), Math.abs(dz)) < r) continue;
                    int x = origin.getX() + dx;
                    int z = origin.getZ() + dz;
                    int y = world.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
                    if (y <= world.getMinBuildHeight()) continue;
                    cursor.set(x, y - 1, z);
                    if (world.getBlockState(cursor).isSolid()) {
                        return new BlockPos(x, y, z);
                    }
                }
            }
        }
        return null;
    }
}
