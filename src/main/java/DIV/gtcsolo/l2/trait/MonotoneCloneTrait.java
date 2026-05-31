package DIV.gtcsolo.l2.trait;

import dev.xkmc.l2hostility.content.capability.mob.MobTraitCap;
import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

/**
 * モノトーンクローン (Monotone Clone) — 周期的に、 特性を持たない自身のクローンを射出する。
 *
 * <p>CD = 36 ÷ (N+1) 秒 (= lv1 で 18 秒、 lv2 で 12 秒…)。 攻撃を受けると CD が 1 秒短縮 (= 削るほど増える)。
 * <p>発火時、 レベル数 (N) 体のクローンを生成。 各クローンは特性なし・ レベルは自身の 75〜100% でランダム・
 *    ランダムな水平方向へ射出。
 *
 * <p>クローンは特性を持たない (= 再帰増殖しない)。 過密時 (= 周囲 mob 40 体以上) は発火を skip。
 */
public class MonotoneCloneTrait extends MobTrait {

    private static final String NEXT_KEY = "gtcsolo.monotone_clone_next";
    private static final int MAX_NEARBY = 40;  // 過密抑止

    public MonotoneCloneTrait(ChatFormatting style) {
        super(style);
    }

    /** CD = 36 ÷ (N+1) 秒 → tick。 */
    private static long cdTicks(int lv) {
        return (long) (36.0 / (lv + 1) * 20);
    }

    @Override
    public void tick(LivingEntity mob, int level) {
        if (mob.level().isClientSide()) return;
        if (!(mob.level() instanceof ServerLevel sl)) return;
        long now = sl.getGameTime();
        var pd = mob.getPersistentData();
        if (!pd.contains(NEXT_KEY)) {
            pd.putLong(NEXT_KEY, now + cdTicks(level));
            return;
        }
        if (now < pd.getLong(NEXT_KEY)) return;
        pd.putLong(NEXT_KEY, now + cdTicks(level));
        spawnClones(sl, mob, level);
    }

    @Override
    public void onHurtByOthers(int level, LivingEntity entity, LivingHurtEvent event) {
        if (entity.level().isClientSide()) return;
        var pd = entity.getPersistentData();
        if (pd.contains(NEXT_KEY)) {
            pd.putLong(NEXT_KEY, pd.getLong(NEXT_KEY) - 20);  // 被弾で CD -1 秒
        }
    }

    private static void spawnClones(ServerLevel sl, LivingEntity mob, int level) {
        if (!MobTraitCap.HOLDER.isProper(mob)) return;
        if (sl.getEntitiesOfClass(Mob.class, mob.getBoundingBox().inflate(32)).size() >= MAX_NEARBY) return;
        int selfLv = MobTraitCap.HOLDER.get(mob).getLevel();
        EntityType<?> type = mob.getType();

        for (int i = 0; i < level; i++) {
            Entity spawned = type.create(sl);
            if (!(spawned instanceof Mob clone)) continue;
            clone.moveTo(mob.getX(), mob.getY(), mob.getZ(), mob.getRandom().nextFloat() * 360f, 0f);
            sl.addFreshEntity(clone);

            // 特性なし + レベル = 自身の 75〜100% (= 「自身の 25% の範囲でランダム」)
            if (MobTraitCap.HOLDER.isProper(clone)) {
                int cloneLv = Math.max(1, (int) (selfLv * (0.75 + mob.getRandom().nextDouble() * 0.25)));
                MobTraitCap cap = MobTraitCap.HOLDER.get(clone);
                cap.reinit(clone, cloneLv, false);  // stage を INIT にして自然 init の trait 付与を抑止
                cap.traits.clear();                  // 特性を持たないクローン (= 再帰増殖しない)
            }

            // ランダムな水平方向へ射出
            double ang = mob.getRandom().nextDouble() * Math.PI * 2;
            clone.setDeltaMovement(Math.cos(ang) * 0.5, 0.35, Math.sin(ang) * 0.5);
            clone.hurtMarked = true;
        }
    }
}
