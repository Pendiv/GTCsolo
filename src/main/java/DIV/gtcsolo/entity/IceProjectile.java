package DIV.gtcsolo.entity;

import DIV.gtcsolo.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;

/**
 * secret_sword Mode 4 用の氷ブロック投擲体。
 *   - 見た目: 氷ブロック (IceProjectileRenderer で BlockRenderer 経由で描画)
 *   - ブロック命中: その面の隣接位置に ice ブロックを設置
 *   - 生物命中: Slow 3 / 120 tick + 20 ダメージ
 *   - どちらも discard
 */
public class IceProjectile extends ThrowableProjectile {

    private static final float DAMAGE = 20.0f;
    private static final int SLOW_DURATION_TICKS = 120;
    private static final int SLOW_AMPLIFIER = 2; // amplifier 2 = Slowness III

    public IceProjectile(EntityType<? extends IceProjectile> type, Level level) {
        super(type, level);
    }

    public IceProjectile(Level level, LivingEntity shooter) {
        super(ModEntities.ICE_PROJECTILE.get(), shooter, level);
    }

    @Override
    protected float getGravity() {
        return 0.04F;
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (level().isClientSide) return;
        BlockPos placePos = result.getBlockPos().relative(result.getDirection());
        BlockState atTarget = level().getBlockState(placePos);
        if (atTarget.canBeReplaced()) {
            level().setBlockAndUpdate(placePos, Blocks.ICE.defaultBlockState());
        }
        this.discard();
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (level().isClientSide) return;
        if (!(result.getEntity() instanceof LivingEntity living)) return;
        DamageSource src = getOwner() instanceof LivingEntity shooter
                ? damageSources().mobAttack(shooter)
                : damageSources().generic();
        living.hurt(src, DAMAGE);
        living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
                SLOW_DURATION_TICKS, SLOW_AMPLIFIER, false, true, true));
        this.discard();
    }

    @Override
    protected void defineSynchedData() {
        // no extra data
    }
}
