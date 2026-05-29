package DIV.gtcsolo.l2.trait;

import DIV.gtcsolo.l2.ModL2Traits;
import DIV.gtcsolo.l2.trait.base.TypedMobTrait;
import dev.xkmc.l2hostility.content.capability.mob.MobTraitCap;
import net.minecraft.ChatFormatting;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.level.ExplosionEvent;

/**
 * [41] Volatile Mix (混ぜ物) — クリーパー専用。 爆発後、 爆心地に残留デバフ雲を放つ。
 *
 * <p>{@link ExplosionEvent.Detonate} を listen し、 source が volatile_mix 持ちクリーパーなら
 * 爆心地に {@link AreaEffectCloud} を生成し、 ランクに応じたデバフを乗せる。
 *
 * <p>デバフプール (= 仕様で確定済): SLOWNESS / POISON / WITHER / BLINDNESS の 4 種。
 * 毎回ランダム選出、 amplifier = N (エフェクトレベル N)、 半径 = (4 + N)、 duration = (7 + 3N²) 秒。
 */
public class VolatileMixTrait extends TypedMobTrait {

    private static final MobEffect[] DEBUFF_POOL = new MobEffect[] {
            MobEffects.MOVEMENT_SLOWDOWN,
            MobEffects.POISON,
            MobEffects.WITHER,
            MobEffects.BLINDNESS,
    };

    public VolatileMixTrait(ChatFormatting style) {
        super(style);
    }

    @Override
    protected boolean isValidTarget(LivingEntity mob) {
        return mob instanceof Creeper;
    }

    /** {@link DIV.gtcsolo.l2.L2EventHandlers#onExplosionDetonate} から呼ばれる */
    public static void onExplosionDetonate(ExplosionEvent.Detonate event) {
        Explosion ex = event.getExplosion();
        Entity src = ex.getDirectSourceEntity();
        if (!(src instanceof Creeper c)) return;
        if (c.level().isClientSide()) return;
        if (!MobTraitCap.HOLDER.isProper(c)) return;
        int lv = MobTraitCap.HOLDER.get(c).getTraitLevel(ModL2Traits.VOLATILE_MIX.get());
        if (lv <= 0) return;

        Vec3 pos = ex.getPosition();
        AreaEffectCloud cloud = new AreaEffectCloud(event.getLevel(), pos.x, pos.y, pos.z);
        cloud.setOwner(c);
        int duration = (7 + 3 * lv * lv) * 20;  // (10 + 3N² - 3) 秒
        cloud.setRadius(4.0f + lv);             // (4 + N) ブロック
        cloud.setDuration(duration);
        cloud.setRadiusPerTick(-cloud.getRadius() / (float) cloud.getDuration());

        RandomSource rng = c.getRandom();
        MobEffect chosen = DEBUFF_POOL[rng.nextInt(DEBUFF_POOL.length)];
        int amplifier = lv - 1;                 // エフェクトレベル N
        cloud.addEffect(new MobEffectInstance(chosen, duration, amplifier));

        event.getLevel().addFreshEntity(cloud);
    }
}
