package DIV.gtcsolo.entity;

import DIV.gtcsolo.registry.ModEntities;
import DIV.gtcsolo.registry.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * ミカン (Orange) を投擲したときの飛翔体。
 *
 * 着弾時に範囲ダメージ + パーティクル演出。 ブロック破壊・延焼なし、 ドロップなし。
 *   - 爆発半径 = end_crystal (6.0) × 1.3 = 7.8 ブロック
 *   - ダメージ = 16 + 1.25 × 射手の ATTACK_DAMAGE 属性値 (素手なら 1.0)
 */
public class OrangeProjectile extends ThrowableItemProjectile {
    // 仕様変更: 爆発範囲と威力を 3倍 (= 旧 7.8 → 23.4、 旧 16+1.25×atk → 48+3.75×atk)
    private static final double EXPLOSION_RADIUS = 6.0 * 1.3 * 3.0;
    private static final float BASE_DAMAGE = 16.0f * 3.0f;
    private static final float ATTACK_DAMAGE_MULTIPLIER = 1.25f * 3.0f;

    public OrangeProjectile(EntityType<? extends OrangeProjectile> type, Level level) {
        super(type, level);
    }

    public OrangeProjectile(Level level, LivingEntity shooter) {
        super(ModEntities.ORANGE_PROJECTILE.get(), shooter, level);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.ORANGE.get();
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!this.level().isClientSide) {
            explode();
            this.discard();
        }
    }

    private void explode() {
        Vec3 pos = this.position();
        Entity owner = this.getOwner();
        float attackDamage = 1.0f;
        if (owner instanceof LivingEntity living) {
            attackDamage = (float) living.getAttributeValue(Attributes.ATTACK_DAMAGE);
        }
        float damage = BASE_DAMAGE + ATTACK_DAMAGE_MULTIPLIER * attackDamage;

        AABB aabb = new AABB(
                pos.x - EXPLOSION_RADIUS, pos.y - EXPLOSION_RADIUS, pos.z - EXPLOSION_RADIUS,
                pos.x + EXPLOSION_RADIUS, pos.y + EXPLOSION_RADIUS, pos.z + EXPLOSION_RADIUS
        );

        DamageSource damageSource;
        if (owner instanceof LivingEntity living) {
            damageSource = this.damageSources().mobAttack(living);
        } else {
            damageSource = this.damageSources().generic();
        }

        for (Entity target : this.level().getEntities(this, aabb)) {
            if (target == owner) continue;
            double distance = target.position().distanceTo(pos);
            if (distance > EXPLOSION_RADIUS) continue;
            target.hurt(damageSource, damage);
        }

        // 視覚効果: 球形に粒子を散らす (server side, particles 自動同期)
        if (this.level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.FLAME, pos.x, pos.y, pos.z, 60, 0.5, 0.5, 0.5, 0.05);
            server.sendParticles(ParticleTypes.LARGE_SMOKE, pos.x, pos.y, pos.z, 30, 0.5, 0.5, 0.5, 0.05);
        }
    }
}
