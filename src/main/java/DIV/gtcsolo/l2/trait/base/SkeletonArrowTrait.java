package DIV.gtcsolo.l2.trait.base;

import DIV.gtcsolo.combat.arrow.SpecialArrow;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Arrow;

/**
 * スケルトン専用「特殊矢を自前 spawn する」 射撃系 trait の共通基底。
 *
 * <p>CD 管理 + 最寄り player への照準発射 ({@link SpecialArrow#aimedArrow}) を提供し、
 * subclass は {@link #cooldown(int)} と {@link #tagArrow(Arrow, int)} (= behavior 付与) だけ実装する。
 *
 * <p>CD は trait の registryName で persistentData に key 化するため、 同一 mob が複数の矢 trait を
 * 持っても CD が干渉しない。 通常 AI の弓射撃とは独立 (= 別個に矢を spawn する)。
 */
public abstract class SkeletonArrowTrait extends TypedMobTrait {

    protected SkeletonArrowTrait(ChatFormatting style) {
        super(style);
    }

    /** CD (tick)。 lv でスケール。 */
    protected abstract long cooldown(int lv);

    /** 生成済みの矢に behavior を付与する ({@code SpecialArrow.put(arrow, behavior[, perf])})。 */
    protected abstract void tagArrow(Arrow arrow, int lv);

    /** 矢の初速 (既定 3.0 = vanilla 弓相当)。 */
    protected float arrowSpeed() { return 3.0f; }

    /** 照準ばらつき (既定 0.5)。 */
    protected float inaccuracy() { return 0.5f; }

    /** 索敵半径 (既定 32)。 */
    protected double radius() { return 32.0; }

    @Override
    protected final boolean isValidTarget(LivingEntity mob) {
        return mob instanceof AbstractSkeleton;
    }

    @Override
    protected final void onValidTick(LivingEntity mob, int lv) {
        if (!(mob.level() instanceof ServerLevel sl)) return;  // server-side guard も兼ねる
        long now = sl.getGameTime();
        String cdKey = "gtcsolo.arrowcd." + getRegistryName().getPath();
        var pd = mob.getPersistentData();
        if (pd.contains(cdKey) && now - pd.getLong(cdKey) < cooldown(lv)) return;
        Player target = sl.getNearestPlayer(mob, radius());
        if (target == null) return;
        pd.putLong(cdKey, now);
        Arrow arrow = SpecialArrow.aimedArrow(mob, target, arrowSpeed(), inaccuracy());
        tagArrow(arrow, lv);
        sl.addFreshEntity(arrow);
    }
}
