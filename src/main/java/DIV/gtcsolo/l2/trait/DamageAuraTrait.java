package DIV.gtcsolo.l2.trait;

import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

/**
 * [53] Damage Aura (ダメージオーラ) — 自身を中心とする 1 辺 A の立方体内に持続的小ダメージ (壁貫通)。
 * 範囲は立方体輪郭パーティクルで描画 → プレイヤーが境界を視認できる。
 *
 * <p>1 辺 A = {@link #SIDE_BASE} + {@link #SIDE_PER_LEVEL} × lv (= lv1 で 8、 lv5 で 16 ブロック)。
 * <p>ダメージ: {@link #DAMAGE_INTERVAL_TICKS} (= 20t) ごとに範囲内全 LivingEntity に
 * {@link #DAMAGE_BASE} + {@link #DAMAGE_PER_LEVEL} × lv (= magic、 壁貫通)。
 * <p>パーティクル: {@link #PARTICLE_INTERVAL_TICKS} (= 10t) ごとに 12 辺をサンプル描画。
 */
public class DamageAuraTrait extends MobTrait {

    private static final int DAMAGE_INTERVAL_TICKS = 20;
    private static final int PARTICLE_INTERVAL_TICKS = 10;
    private static final int EDGE_SAMPLES = 5;

    public DamageAuraTrait(ChatFormatting style) {
        super(style);
    }

    @Override
    public void tick(LivingEntity mob, int level) {
        if (mob.level().isClientSide()) return;
        if (!(mob.level() instanceof ServerLevel sl)) return;
        int t = mob.tickCount;
        double half = (4.0 + level) / 2.0;  // 1 辺 (4 + N) ブロックの立方体
        if (t % PARTICLE_INTERVAL_TICKS == 0) {
            emitCubeOutline(sl, mob.getX(), mob.getY(), mob.getZ(), half);
        }
        if (t % DAMAGE_INTERVAL_TICKS != 0) return;
        AABB area = new AABB(
                mob.getX() - half, mob.getY() - half, mob.getZ() - half,
                mob.getX() + half, mob.getY() + half, mob.getZ() + half);
        DamageSource src = mob.damageSources().magic();
        net.minecraft.world.entity.ai.attributes.AttributeInstance atkInst =
                mob.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
        double atk = atkInst != null ? atkInst.getValue() : 0.0;
        float amount = 3.0f + (float) (atk * 0.25 * level);  // 3 + 攻撃力 × 25N% の魔法ダメージ
        for (LivingEntity victim : sl.getEntitiesOfClass(LivingEntity.class, area, e -> e != mob)) {
            victim.hurt(src, amount);
        }
    }

    /** 立方体 12 辺に沿ってパーティクル散布 — 各軸 4 辺 × {@link #EDGE_SAMPLES} サンプル */
    private static void emitCubeOutline(ServerLevel sl, double cx, double cy, double cz, double h) {
        for (int i = 0; i <= EDGE_SAMPLES; i++) {
            double t = (i / (double) EDGE_SAMPLES) * 2 - 1; // -1..1
            double p = t * h;
            // X 方向 4 辺: (p, ±h, ±h)
            spawn(sl, cx + p, cy + h, cz + h);
            spawn(sl, cx + p, cy + h, cz - h);
            spawn(sl, cx + p, cy - h, cz + h);
            spawn(sl, cx + p, cy - h, cz - h);
            // Y 方向 4 辺: (±h, p, ±h)
            spawn(sl, cx + h, cy + p, cz + h);
            spawn(sl, cx + h, cy + p, cz - h);
            spawn(sl, cx - h, cy + p, cz + h);
            spawn(sl, cx - h, cy + p, cz - h);
            // Z 方向 4 辺: (±h, ±h, p)
            spawn(sl, cx + h, cy + h, cz + p);
            spawn(sl, cx + h, cy - h, cz + p);
            spawn(sl, cx - h, cy + h, cz + p);
            spawn(sl, cx - h, cy - h, cz + p);
        }
    }

    private static void spawn(ServerLevel sl, double x, double y, double z) {
        sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, x, y, z, 1, 0, 0, 0, 0);
    }
}
