package DIV.gtcsolo.combat.arrow;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * 組み込み特殊矢 behavior 群。 各定数が 1 種の矢を表す。
 *
 * <p>性能 (performance) の意味と初期値は仕様 (refs/claude/2026-05-29_arrow_system_and_new_traits_spec.md)
 * に準拠。 数値が未確定の項目は妥当な仮値を置きコメントで明示 (= 後で調整)。
 */
public enum ArrowBehaviors implements ArrowBehavior {

    /**
     * 鈍足の矢 — 着弾対象に鈍足を 10 秒付与。
     * 性能 = Slowness レベル相当 (= amplifier = round(perf) - 1)。 初期値 1.0 → Slowness I。
     * [要調整] 「鈍足100%」 の amplifier 換算 (immobile を意図するなら高 amplifier に)。
     */
    SLOWNESS("slowness", 1.0) {
        @Override
        public void onHitEntity(AbstractArrow arrow, LivingEntity target, @Nullable LivingEntity shooter, double perf) {
            int amp = Math.max(0, (int) Math.round(perf) - 1);
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, SLOWNESS_DURATION, amp));
        }
    },

    /**
     * 飛燕の矢 — 対象に高 Lv 浮遊を付与し上方へ吹き飛ばす。
     * 性能 = Levitation レベル相当 (amplifier = round(perf) - 1)。 初期値 5.0 → Levitation V。
     */
    SWALLOW("swallow", 5.0) {
        @Override
        public void onHitEntity(AbstractArrow arrow, LivingEntity target, @Nullable LivingEntity shooter, double perf) {
            int amp = Math.max(0, (int) Math.round(perf) - 1);
            target.addEffect(new MobEffectInstance(MobEffects.LEVITATION, SWALLOW_DURATION, amp));
            Vec3 v = target.getDeltaMovement();
            target.setDeltaMovement(v.x, Math.max(v.y, SWALLOW_LAUNCH), v.z);
            target.hurtMarked = true;
        }
    },

    /**
     * 反発矢 — 対象を吹き飛ばす。 性能 = 吹き飛ばし威力 (初期値 5.0)。
     * 吹き飛ばし方向 = 矢の進行方向。 [要調整] 威力→knockback strength 係数。
     */
    REPULSION("repulsion", 5.0) {
        @Override
        public void onHitEntity(AbstractArrow arrow, LivingEntity target, @Nullable LivingEntity shooter, double perf) {
            Vec3 dir = arrow.getDeltaMovement();
            double dx = dir.x, dz = dir.z;
            double len = Math.sqrt(dx * dx + dz * dz);
            if (len < 1.0e-4) return;
            double strength = perf * REPULSION_FACTOR;
            // knockback(strength, x, z) は (x,z) と逆方向へ押す → 矢の進行方向へ飛ばすには符号反転
            target.knockback(strength, -dx / len, -dz / len);
            target.hurtMarked = true;
        }
    },

    /**
     * 弩の矢 — 強ノックバック。 ノックバック耐性を部分貫通 ({@link #CROSSBOW_RESIST_PIERCE} 分だけ無視)。
     * 吹き飛ばし後、 対象 player に一定時間 弩耐性を付与 (= 連続吹き飛ばしの拘束を防ぐ)。 性能 = KB 威力。
     */
    CROSSBOW("crossbow", 5.0) {
        @Override
        public void onHitEntity(AbstractArrow arrow, LivingEntity target, @Nullable LivingEntity shooter, double perf) {
            Vec3 dir = arrow.getDeltaMovement();
            double dx = dir.x, dz = dir.z;
            double len = Math.sqrt(dx * dx + dz * dz);
            if (len < 1.0e-4) return;
            if (target instanceof Player p) {
                long now = p.level().getGameTime();
                if (p.getPersistentData().getLong(CROSSBOW_IMMUNE_KEY) > now) return;  // 弩耐性窓中はKB無し
                p.getPersistentData().putLong(CROSSBOW_IMMUNE_KEY, now + CROSSBOW_IMMUNE_TICKS);
            }
            double resist = 0.0;
            var inst = target.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
            if (inst != null) resist = inst.getValue();
            // KB 耐性を部分貫通: 実効倍率 = 1 - 耐性 × (1 - pierce)
            double strength = perf * CROSSBOW_KB_FACTOR
                    * Math.max(0.0, 1.0 - resist * (1.0 - CROSSBOW_RESIST_PIERCE));
            Vec3 v = target.getDeltaMovement();
            target.setDeltaMovement(v.x + dx / len * strength, Math.max(v.y, 0.42), v.z + dz / len * strength);
            target.hurtMarked = true;
        }
    },

    /**
     * 爆裂矢 — プレイヤー or 壁への着弾で爆発。 性能 = 爆発威力 (初期値 5.0)。
     * ブロック破壊なし (= 仕様の「未指定=破壊しない」)。 破壊版は別途 (single-perf モデルゆえ behavior 分離で対応予定)。
     */
    EXPLOSIVE("explosive", 5.0) {
        @Override
        public void onHitEntity(AbstractArrow arrow, LivingEntity target, @Nullable LivingEntity shooter, double perf) {
            explode(arrow, arrow.getX(), arrow.getY(), arrow.getZ(), (float) perf);
        }
        @Override
        public void onHitBlock(AbstractArrow arrow, BlockHitResult hit, @Nullable LivingEntity shooter, double perf) {
            explode(arrow, hit.getLocation().x, hit.getLocation().y, hit.getLocation().z, (float) perf);
        }
    },

    /**
     * 撃滅の矢 — 防御貫通。 性能 = 貫通率 (0〜1、 初期値 1.0 = 100%)。
     * AllOrNothing 式の近似: post-armor の amount に target の防具値 × 貫通率を足し戻す。
     */
    ANNIHILATION("annihilation", 1.0) {
        @Override
        public void onHurt(AbstractArrow arrow, net.minecraftforge.event.entity.living.LivingHurtEvent event,
                           @Nullable LivingEntity shooter, double perf) {
            double armor = event.getEntity().getArmorValue();
            if (armor <= 0) return;
            event.setAmount((float) (event.getAmount() + armor * perf));
        }
    },

    /**
     * 雷矢 (= 雷使いが使用) — 着弾対象の座標に雷を落とし、 対象の攻撃力を参照した追加ダメージを与える。
     * 性能 = 攻撃力参照倍率 (初期値 1.0 = 100%)。
     * <p>[注] 実体の {@link LightningBolt} を落とすため地形着火等の副作用あり (= 必要なら visual-only 化)。
     */
    LIGHTNING("lightning", 1.0) {
        @Override
        public void onHitEntity(AbstractArrow arrow, LivingEntity target, @Nullable LivingEntity shooter, double perf) {
            if (target.level() instanceof ServerLevel sl) {
                LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(sl);
                if (bolt != null) {
                    bolt.moveTo(target.getX(), target.getY(), target.getZ());
                    sl.addFreshEntity(bolt);
                }
            }
            var inst = target.getAttribute(Attributes.ATTACK_DAMAGE);
            if (inst != null) {
                float bonus = (float) (inst.getValue() * perf);
                if (bonus > 0) {
                    DamageSource src = arrow.damageSources().magic();
                    target.hurt(src, bonus);
                }
            }
        }
    },

    /**
     * 飛墜の矢 — 着弾した player の飛行を一定時間無効化 (エリトラ滑空 / クリエイティブ飛行 / MOD 飛行)。
     * 性能 = 無効化 tick 数 (初期値 200 = 10 秒)。 実際の抑止は {@link ArrowEventHandlers} の tick が行う。
     */
    PLUMMET("plummet", 200.0) {
        @Override
        public void onHitEntity(AbstractArrow arrow, LivingEntity target, @Nullable LivingEntity shooter, double perf) {
            if (!(target instanceof Player p)) return;
            long until = p.level().getGameTime() + (long) perf;
            p.getPersistentData().putLong(ArrowEventHandlers.NO_FLY_KEY, until);
        }
    },

    /**
     * 帰結の矢 — 飛翔フェイズをキャンセルし発射方向へ即着弾 (hitscan)。 性能 = 最大射程 (初期値 128)。
     * onSpawn で進行方向に raytrace し、 最初の衝突点直前へ矢を移動 → 次 tick で即着弾する。
     */
    CONSEQUENCE("consequence", 128.0) {
        @Override
        public void onSpawn(AbstractArrow arrow, @Nullable LivingEntity shooter, double perf) {
            Vec3 vel = arrow.getDeltaMovement();
            if (vel.lengthSqr() < 1.0e-4) return;
            Vec3 dir = vel.normalize();
            Vec3 start = arrow.position();
            Vec3 end = start.add(dir.scale(perf));

            // ブロック raycast (= 壁で停止)
            BlockHitResult block = arrow.level().clip(new ClipContext(
                    start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, arrow));
            Vec3 limit = block.getType() != HitResult.Type.MISS ? block.getLocation() : end;

            // entity raycast (= 壁手前までの最初の LivingEntity)
            AABB box = arrow.getBoundingBox().expandTowards(dir.scale(perf)).inflate(1.0);
            EntityHitResult entity = ProjectileUtil.getEntityHitResult(arrow.level(), arrow, start, limit, box,
                    e -> e instanceof LivingEntity && e != shooter && !e.isSpectator());
            Vec3 hit = entity != null ? entity.getLocation() : limit;

            // 衝突点直前へ移動 → 次 tick で travel 0.1 して着弾
            Vec3 placed = hit.subtract(dir.scale(0.1));
            arrow.setPos(placed.x, placed.y, placed.z);
        }
    };

    // ── 共通仮値 (= 後で調整) ──
    private static final int SLOWNESS_DURATION = 200;   // 10 秒
    private static final int SWALLOW_DURATION = 60;     // 浮遊 3 秒
    private static final double SWALLOW_LAUNCH = 0.8;    // 上方初速
    private static final double REPULSION_FACTOR = 0.4;  // 威力 → knockback strength 係数 (perf5 → 2.0)
    private static final double CROSSBOW_KB_FACTOR = 0.35;     // 弩 威力 → strength 係数
    private static final double CROSSBOW_RESIST_PIERCE = 0.5;  // KB 耐性の貫通率 (0.5 = 耐性の半分を無視)
    private static final long CROSSBOW_IMMUNE_TICKS = 30L;     // 吹き飛ばし後の弩耐性窓 (= 1.5 秒)
    private static final String CROSSBOW_IMMUNE_KEY = "gtcsolo.crossbow_immune_until";

    private final String id;
    private final double defaultPerformance;

    ArrowBehaviors(String id, double defaultPerformance) {
        this.id = id;
        this.defaultPerformance = defaultPerformance;
    }

    @Override public String id() { return id; }
    @Override public double defaultPerformance() { return defaultPerformance; }

    /** ブロック非破壊・非着火の爆発。 */
    private static void explode(AbstractArrow arrow, double x, double y, double z, float power) {
        Entity owner = arrow.getOwner();
        arrow.level().explode(owner != null ? owner : arrow, x, y, z, power, false,
                Level.ExplosionInteraction.NONE);
    }

    /** 組み込み behavior を {@link SpecialArrow} に登録する。 */
    public static void registerAll() {
        for (ArrowBehaviors b : values()) {
            SpecialArrow.register(b);
        }
    }
}
