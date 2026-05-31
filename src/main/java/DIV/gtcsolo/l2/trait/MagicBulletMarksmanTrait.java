package DIV.gtcsolo.l2.trait;

import DIV.gtcsolo.combat.arrow.ArrowBehavior;
import DIV.gtcsolo.combat.arrow.ArrowBehaviors;
import DIV.gtcsolo.combat.arrow.SpecialArrow;
import DIV.gtcsolo.l2.trait.base.TypedMobTrait;
import DIV.gtcsolo.l2.util.L2TraitAttributes;
import dev.xkmc.l2damagetracker.init.L2DamageTracker;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

import java.util.UUID;

/**
 * 魔弾の射手 (Magic Bullet Marksman) — スケルトン専用のボス級射手。
 *
 * <ul>
 *   <li>postInit: 攻撃力・最大 HP +(10+5N)% (MULTIPLY_BASE)、 ダメージ軽減 +(10+5N)% (L2DT REDUCTION)</li>
 *   <li>player が接近 ({@value #CLOSE_DIST} 内) すると透明化 + 移動速度上昇</li>
 *   <li>発射ごとに Lv×7% + 累積ボーナス の確率で特殊な矢 (プールからランダム・性能は初期値固定) を放つ</li>
 *   <li>特殊な矢を放たなかった時は確率 +4%、 被弾でも +3% 累積 (= Lv 非参照)。 特殊な矢を放つと累積は失効し 0 に戻る</li>
 * </ul>
 *
 * <p>[保留] 仕様の「player 周辺のブロックを貫通する矢」 は未実装。
 */
public class MagicBulletMarksmanTrait extends TypedMobTrait {

    private static final UUID MOD_ATK = UUID.fromString("c9f2a1d4-3e6b-4a8c-9d17-2f5b8e1c3a40");
    private static final UUID MOD_HP = UUID.fromString("c9f2a1d4-3e6b-4a8c-9d17-2f5b8e1c3a41");
    private static final UUID MOD_RED = UUID.fromString("c9f2a1d4-3e6b-4a8c-9d17-2f5b8e1c3a42");
    private static final UUID MOD_SPEED = UUID.fromString("c9f2a1d4-3e6b-4a8c-9d17-2f5b8e1c3a43");

    private static final String CD_KEY = "gtcsolo.magic_bullet_cd";
    private static final String BONUS_KEY = "gtcsolo.magic_bullet_bonus";

    private static final double RADIUS = 48.0;
    private static final double CLOSE_DIST = 12.0;
    private static final long FIRE_CD = 40L;            // 2 秒ごとに 1 射
    private static final double BASE_PER_LEVEL = 0.07;  // 特殊矢 確率 = Lv × 7%
    private static final double MISS_GAIN = 0.04;        // 外すと +4%
    private static final double HIT_GAIN = 0.03;         // 被弾で +3%

    /** 特殊な矢プール (= 性能は全て初期値固定で放つ)。 */
    private static final ArrowBehavior[] POOL = {
            ArrowBehaviors.EXPLOSIVE, ArrowBehaviors.LIGHTNING, ArrowBehaviors.SLOWNESS,
            ArrowBehaviors.ANNIHILATION, ArrowBehaviors.PLUMMET, ArrowBehaviors.SWALLOW
    };

    public MagicBulletMarksmanTrait(ChatFormatting style) {
        super(style);
    }

    @Override
    protected boolean isValidTarget(LivingEntity mob) {
        return mob instanceof AbstractSkeleton;
    }

    @Override
    protected void onValidPostInit(LivingEntity mob, int lv) {
        double pct = 0.10 + 0.05 * lv;  // (10 + 5N)%
        L2TraitAttributes.addPermanentIfAbsent(mob, Attributes.ATTACK_DAMAGE, MOD_ATK, "gtcsolo.magic_bullet_atk",
                pct, AttributeModifier.Operation.MULTIPLY_BASE);
        L2TraitAttributes.addPermanentIfAbsent(mob, Attributes.MAX_HEALTH, MOD_HP, "gtcsolo.magic_bullet_hp",
                pct, AttributeModifier.Operation.MULTIPLY_BASE);
        L2TraitAttributes.addPermanentIfAbsent(mob, L2DamageTracker.REDUCTION.get(), MOD_RED, "gtcsolo.magic_bullet_red",
                pct, AttributeModifier.Operation.ADDITION);
        mob.setHealth(mob.getMaxHealth());
    }

    @Override
    protected void onValidTick(LivingEntity mob, int lv) {
        if (!(mob.level() instanceof ServerLevel sl)) return;
        Player near = sl.getNearestPlayer(mob, RADIUS);

        // 接近で透明化 + 移動速度上昇
        boolean close = near != null && near.distanceTo(mob) <= CLOSE_DIST;
        if (close) {
            if (!mob.isInvisible()) mob.setInvisible(true);
            L2TraitAttributes.setTransient(mob, Attributes.MOVEMENT_SPEED, MOD_SPEED, "gtcsolo.magic_bullet_speed",
                    0.5, AttributeModifier.Operation.MULTIPLY_BASE);
        } else {
            if (mob.isInvisible()) mob.setInvisible(false);
            L2TraitAttributes.remove(mob, Attributes.MOVEMENT_SPEED, MOD_SPEED);
        }

        if (near == null) return;

        // 発射 CD
        long now = sl.getGameTime();
        var pd = mob.getPersistentData();
        if (pd.contains(CD_KEY) && now - pd.getLong(CD_KEY) < FIRE_CD) return;
        pd.putLong(CD_KEY, now);

        RandomSource rng = mob.getRandom();
        double prob = lv * BASE_PER_LEVEL + pd.getDouble(BONUS_KEY);
        Arrow arrow = SpecialArrow.aimedArrow(mob, near, 3.0f, 1.0f);
        if (rng.nextDouble() < prob) {
            // 特殊な矢 (= プールからランダム・性能初期値固定) → 累積失効
            SpecialArrow.put(arrow, POOL[rng.nextInt(POOL.length)]);
            pd.putDouble(BONUS_KEY, 0.0);
        } else {
            // 通常矢 → 確率累積 +4%
            pd.putDouble(BONUS_KEY, pd.getDouble(BONUS_KEY) + MISS_GAIN);
        }
        sl.addFreshEntity(arrow);
    }

    @Override
    protected void onValidHurtByOthers(int level, LivingEntity entity, LivingHurtEvent event) {
        if (entity.level().isClientSide()) return;
        var pd = entity.getPersistentData();
        pd.putDouble(BONUS_KEY, pd.getDouble(BONUS_KEY) + HIT_GAIN);  // 被弾で +3% 累積 (Lv 非参照)
    }
}
