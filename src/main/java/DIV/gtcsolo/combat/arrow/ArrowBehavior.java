package DIV.gtcsolo.combat.arrow;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

import javax.annotation.Nullable;

/**
 * 特殊な矢が載せる 1 効果 (= behavior)。 1 本の矢は複数 behavior を持てる。
 *
 * <p>各 behavior は <b>性能 (performance)</b> という単一スカラ (double) でスケールする。
 * 呼び出し側 (スキル/特性) は {@link SpecialArrow#put} で性能を指定し、 未指定なら
 * {@link #defaultPerformance()} (= 初期値) を使う。 性能の意味は behavior ごとに異なる
 * (= 百分率、 倍率、 amplifier 換算元 等)。
 *
 * <p>合成: behavior は実装内で他 behavior を呼べる
 * (例 不動の矢 = {@code ArrowBehaviors.SLOWNESS.onHitEntity(arrow, target, shooter, defaultPerformance())})。
 *
 * <p>全 hook は server-side でのみ呼ばれる (= {@link ArrowEventHandlers} が clientSide を弾く)。
 * 必要な hook だけ override する (既定は no-op)。
 */
public interface ArrowBehavior {

    /** NBT キー兼レジストリ ID (= 一意)。 */
    String id();

    /** 性能の初期値。 {@link SpecialArrow#put(AbstractArrow, ArrowBehavior)} で使われる。 */
    double defaultPerformance();

    /** 矢の出現時 (= 発射直後)。 hitscan 化 (帰結の矢) や初速調整に使う。 */
    default void onSpawn(AbstractArrow arrow, @Nullable LivingEntity shooter, double performance) {}

    /** entity への着弾時 (= ProjectileImpactEvent / EntityHitResult)。 vanilla ダメージ適用の<b>前</b>。 */
    default void onHitEntity(AbstractArrow arrow, LivingEntity target, @Nullable LivingEntity shooter, double performance) {}

    /** ブロックへの着弾時 (= ProjectileImpactEvent / BlockHitResult)。 */
    default void onHitBlock(AbstractArrow arrow, BlockHitResult hit, @Nullable LivingEntity shooter, double performance) {}

    /**
     * 矢由来ダメージの {@link LivingHurtEvent} 中 (= vanilla の防御計算後、 適用前)。
     * ダメージ値を改変する behavior 用 (= 防御貫通など)。
     */
    default void onHurt(AbstractArrow arrow, LivingHurtEvent event, @Nullable LivingEntity shooter, double performance) {}

    /**
     * 飛翔中の毎 tick (= homing 等)。 当面 {@link ArrowEventHandlers} 側は未配線。
     * @return 追従継続が必要なら true (将来の tick dispatcher 用)
     */
    default boolean onTick(AbstractArrow arrow, double performance) { return false; }
}
