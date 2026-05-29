package DIV.gtcsolo.l2.trait;

import DIV.gtcsolo.l2.ModL2Traits;
import DIV.gtcsolo.l2.trait.base.TypedMobTrait;
import dev.xkmc.l2hostility.content.capability.mob.MobTraitCap;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * [44] Homing Shot (ホーミング弾) — スケルトン専用。 放った矢に緩やかな追尾補正がかかる。
 *
 * <p>{@link EntityJoinLevelEvent} で arrow を mark (= persistentData に flag + level)、
 * server tick で全 marked arrow を iterate して player 方向に微補正。
 *
 * <p>マーク管理: 静的 map (= dim → marked arrow UUID set)。
 */
public class HomingShotTrait extends TypedMobTrait {

    public static final String NBT_HOMING_LEVEL = "gtcsolo.homing_shot_lv";

    /** dim resource location → marked arrow UUID set */
    private static final Map<String, Set<UUID>> MARKED = new ConcurrentHashMap<>();
    private static final double SEARCH_RADIUS = 48.0;
    private static final double BASE_CORRECTION = 0.02;  // 弱い追従で固定 (level 非依存)

    public HomingShotTrait(ChatFormatting style) {
        super(style);
    }

    @Override
    protected boolean isValidTarget(LivingEntity mob) {
        return mob instanceof AbstractSkeleton;
    }

    /** {@link DIV.gtcsolo.l2.L2EventHandlers#onEntityJoinLevel} から呼ばれる */
    public static void onArrowJoin(EntityJoinLevelEvent event) {
        Entity e = event.getEntity();
        if (!(e instanceof AbstractArrow arrow)) return;
        if (event.getLevel().isClientSide()) return;
        Entity owner = arrow.getOwner();
        if (!(owner instanceof AbstractSkeleton skel)) return;
        if (!MobTraitCap.HOLDER.isProper(skel)) return;
        int lv = MobTraitCap.HOLDER.get(skel).getTraitLevel(ModL2Traits.HOMING_SHOT.get());
        if (lv <= 0) return;
        CompoundTag tag = arrow.getPersistentData();
        tag.putInt(NBT_HOMING_LEVEL, lv);
        String dim = event.getLevel().dimension().location().toString();
        MARKED.computeIfAbsent(dim, k -> ConcurrentHashMap.newKeySet()).add(arrow.getUUID());
    }

    /** {@link DIV.gtcsolo.l2.L2EventHandlers#onServerTick} から呼ばれる (毎 tick) */
    public static void serverTick(MinecraftServer server) {
        for (ServerLevel sl : server.getAllLevels()) {
            Set<UUID> set = MARKED.get(sl.dimension().location().toString());
            if (set == null || set.isEmpty()) continue;
            Iterator<UUID> it = set.iterator();
            while (it.hasNext()) {
                UUID id = it.next();
                Entity ent = sl.getEntity(id);
                if (!(ent instanceof AbstractArrow arrow) || !arrow.isAlive()) {
                    it.remove();
                    continue;
                }
                // 地中に刺さった矢は速度ゼロになるので追尾停止
                if (arrow.getDeltaMovement().lengthSqr() < 0.0001) {
                    it.remove();
                    continue;
                }
                int lv = arrow.getPersistentData().getInt(NBT_HOMING_LEVEL);
                if (lv <= 0) {
                    it.remove();
                    continue;
                }
                adjustTrajectory(arrow, lv);
            }
        }
    }

    private static void adjustTrajectory(AbstractArrow arrow, int lv) {
        Player target = arrow.level().getNearestPlayer(arrow, SEARCH_RADIUS);
        if (target == null) return;
        Vec3 desired = target.position().add(0, target.getBbHeight() / 2.0, 0)
                .subtract(arrow.position()).normalize();
        Vec3 cur = arrow.getDeltaMovement();
        double speed = cur.length();
        double correction = BASE_CORRECTION;
        Vec3 blended = cur.scale(1.0 - correction).add(desired.scale(speed * correction));
        // 合成後 vector を元 speed に正規化 (= 速度維持)
        Vec3 newVel = blended.normalize().scale(speed);
        arrow.setDeltaMovement(newVel);
        arrow.hurtMarked = true;
    }
}
