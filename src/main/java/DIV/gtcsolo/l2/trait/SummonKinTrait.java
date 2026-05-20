package DIV.gtcsolo.l2.trait;

import DIV.gtcsolo.l2.ModL2Traits;
import DIV.gtcsolo.l2.trait.base.TypedMobTrait;
import dev.xkmc.l2hostility.content.capability.mob.MobTraitCap;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Creeper;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * [43] Summon Kin (類は爆発を呼ぶ) — クリーパー専用。
 * 一定時間ごとに別個体のクリーパーを召喚する (親子関係なし)。
 * 平均 9 割で同特性を継承させる。
 *
 * <p>クールタイム = {@link #BASE_COOLDOWN_TICKS} - lv × {@link #COOLDOWN_REDUCTION_PER_LEVEL}
 * (= lv1 で 30 秒、 lv5 で 14 秒、 最低 5 秒)。
 */
public class SummonKinTrait extends TypedMobTrait {

    private static final Map<UUID, Long> LAST_SUMMON = new HashMap<>();
    private static final long BASE_COOLDOWN_TICKS = 600L;
    private static final long COOLDOWN_REDUCTION_PER_LEVEL = 80L;
    private static final long MIN_COOLDOWN_TICKS = 100L;
    private static final double INHERIT_CHANCE = 0.9;
    /** 過密抑止: 半径 32 内にこの数の creeper が居たら summon スキップ */
    private static final int MAX_NEARBY_CREEPERS = 8;

    public SummonKinTrait(ChatFormatting style) {
        super(style);
    }

    @Override
    protected boolean isValidTarget(LivingEntity mob) {
        return mob instanceof Creeper;
    }

    @Override
    protected void onValidTick(LivingEntity mob, int lv) {
        if (mob.level().isClientSide()) return;
        if (!(mob.level() instanceof ServerLevel sl)) return;
        long now = sl.getGameTime();
        long cooldown = Math.max(MIN_COOLDOWN_TICKS,
                BASE_COOLDOWN_TICKS - COOLDOWN_REDUCTION_PER_LEVEL * lv);
        Long last = LAST_SUMMON.get(mob.getUUID());
        // 新生 mob は CD 未登録 = null。 ここで now を登録して全 mob が初回 CD 経過後に発動するように
        // 揃える (= 旧コードでは null を「即時発動可」 と誤解釈 → 新生 kin が即増殖 → 指数関数的爆発)
        if (last == null) {
            LAST_SUMMON.put(mob.getUUID(), now);
            return;
        }
        if (now - last < cooldown) return;

        // 過密抑止: 半径 32 内に creeper が多すぎたら summon 自体スキップ (CD だけリセット)
        int nearbyCreepers = sl.getEntitiesOfClass(Creeper.class,
                mob.getBoundingBox().inflate(32.0)).size();
        if (nearbyCreepers >= MAX_NEARBY_CREEPERS) {
            LAST_SUMMON.put(mob.getUUID(), now);
            return;
        }

        LAST_SUMMON.put(mob.getUUID(), now);
        EntityType<Creeper> type = EntityType.CREEPER;
        Entity spawned = type.spawn(sl, mob.blockPosition(), MobSpawnType.MOB_SUMMONED);
        if (!(spawned instanceof Creeper kin)) return;
        // 新生 kin にも CD を初期登録 — 次 tick で「last == null」 になって即増殖するのを防ぐ
        LAST_SUMMON.put(kin.getUUID(), now);
        if (mob.getRandom().nextDouble() < INHERIT_CHANCE
                && MobTraitCap.HOLDER.isProper(kin)) {
            MobTraitCap cap = MobTraitCap.HOLDER.get(kin);
            cap.setTrait(ModL2Traits.SUMMON_KIN.get(), lv);
        }
    }
}
