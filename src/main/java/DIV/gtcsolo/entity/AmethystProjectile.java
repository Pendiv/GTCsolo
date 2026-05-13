package DIV.gtcsolo.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * secret_sword Mode 6: 地形・モブ貫通アメジスト。
 *   - 32m 飛翔したら消滅
 *   - 地形貫通 (noPhysics=true)、 モブも貫通 (= 同じ entity に再ヒットしないだけ)
 *   - 当たった mob に 20 ダメージ
 *   - 視覚は GLOW パーティクル trail (= 専用 renderer は不要)
 */
public class AmethystProjectile extends Entity {

    private static final double MAX_DISTANCE = 32.0;
    private static final float DAMAGE = 20.0f;

    private LivingEntity shooter;
    private double traveled;
    private final Set<UUID> hitEntities = new HashSet<>();

    public AmethystProjectile(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public void setShooter(LivingEntity shooter) {
        this.shooter = shooter;
    }

    public void shoot(Vec3 direction, double speed) {
        Vec3 v = direction.normalize().scale(speed);
        setDeltaMovement(v);
    }

    @Override
    public void tick() {
        super.tick();
        Vec3 motion = getDeltaMovement();
        double step = motion.length();
        Vec3 newPos = position().add(motion);
        traveled += step;
        if (traveled > MAX_DISTANCE) {
            this.discard();
            return;
        }

        if (!level().isClientSide) {
            AABB hitArea = getBoundingBox().expandTowards(motion).inflate(0.3);
            for (Entity entity : level().getEntities(this, hitArea,
                    e -> e instanceof LivingEntity && e != shooter)) {
                if (!hitEntities.add(entity.getUUID())) continue;
                DamageSource src = shooter != null
                        ? damageSources().mobAttack(shooter)
                        : damageSources().generic();
                entity.hurt(src, DAMAGE);
            }
            if (level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.GLOW, newPos.x, newPos.y, newPos.z,
                        2, 0.05, 0.05, 0.05, 0.0);
                sl.sendParticles(ParticleTypes.WAX_OFF, newPos.x, newPos.y, newPos.z,
                        1, 0.0, 0.0, 0.0, 0.0);
            }
        }

        setPos(newPos);
    }

    @Override
    protected void defineSynchedData() {}

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        traveled = tag.getDouble("traveled");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putDouble("traveled", traveled);
    }
}
