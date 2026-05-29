package DIV.gtcsolo.l2.trait;

import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * [55] Bomber Dispatch (爆弾魔派遣業) — 汎用。
 * たまに最寄りプレイヤー目掛けて着火済みクリーパーを投擲する。
 *
 * <p>クールタイム = 240 + 360÷n tick (= lv1 で 30 秒、 lv2 で 21 秒、 lv3 で 18 秒)。 max_rank 3。
 * <p>投擲: 自分の頭上にクリーパーを生成、 ignite() 済み、 player 方向に
 * setDeltaMovement で射出 (速度 = {@link #BASE_THROW_SPEED} 一定)。
 */
public class BomberDispatchTrait extends MobTrait {

    private static final Map<UUID, Long> LAST_THROW = new HashMap<>();
    private static final double SEARCH_RADIUS = 24.0;
    private static final double BASE_THROW_SPEED = 0.8;

    public BomberDispatchTrait(ChatFormatting style) {
        super(style);
    }

    @Override
    public void tick(LivingEntity mob, int lv) {
        if (mob.level().isClientSide()) return;
        if (!(mob.level() instanceof ServerLevel sl)) return;
        long now = sl.getGameTime();
        long cooldown = 240 + 360 / lv;  // 240 + 360÷n tick
        Long last = LAST_THROW.get(mob.getUUID());
        if (last != null && now - last < cooldown) return;
        Player p = sl.getNearestPlayer(mob, SEARCH_RADIUS);
        if (p == null) return;
        LAST_THROW.put(mob.getUUID(), now);

        Creeper bomb = EntityType.CREEPER.create(sl);
        if (bomb == null) return;
        bomb.setPos(mob.getX(), mob.getY() + 1.5, mob.getZ());
        bomb.ignite();
        // player 方向 + 上向き補正
        Vec3 dir = p.position().subtract(mob.position()).normalize();
        double speed = BASE_THROW_SPEED;  // 一定 (level 非依存)
        bomb.setDeltaMovement(dir.x * speed, dir.y * speed + 0.3, dir.z * speed);
        sl.addFreshEntity(bomb);
    }
}
