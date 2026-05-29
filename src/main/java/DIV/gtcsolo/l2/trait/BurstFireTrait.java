package DIV.gtcsolo.l2.trait;

import DIV.gtcsolo.l2.trait.base.TypedMobTrait;
import dev.xkmc.l2hostility.content.capability.mob.CapStorageData;
import dev.xkmc.l2hostility.content.capability.mob.MobTraitCap;
import dev.xkmc.l2serial.serialization.SerialClass;
import net.minecraft.ChatFormatting;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.RangedBowAttackGoal;
import net.minecraft.world.entity.monster.AbstractSkeleton;

import java.lang.reflect.Field;

/**
 * [46] Burst Fire (速射) — スケルトン専用。
 * (10 + 5N) 発のあいだ射撃間隔を短縮し、 撃ち切ると (75 - 10N) 秒のクールタイムに入る。
 *
 * <p>仕様: 発射速度 +(75 + 1.3n)% (= interval ÷ rateMult)、 (10 + 5N) 発分継続。
 * <p>shot count は射撃検出が困難なため、 burstInterval × shots の tick 窓で近似。
 * クールタイム = (75 - 10N) 秒。
 *
 * <p>排他: [45] Rapid Fire と排他 (連射機構衝突)、 datapack 側で配布絞り推奨。
 */
public class BurstFireTrait extends TypedMobTrait {

    private static final Field ATTACK_INTERVAL_FIELD = RapidFireTrait.getAttackIntervalField();

    public BurstFireTrait(ChatFormatting style) {
        super(style);
    }

    @Override
    protected boolean isValidTarget(LivingEntity mob) {
        return mob instanceof AbstractSkeleton;
    }

    @Override
    protected void onValidTick(LivingEntity mob, int lv) {
        if (ATTACK_INTERVAL_FIELD == null) return;
        if (mob.level().isClientSide()) return;
        if (!(mob instanceof Mob m)) return;
        if (!MobTraitCap.HOLDER.isProper(mob)) return;
        boolean inCombat = m.getTarget() != null && m.getTarget().isAlive();
        MobTraitCap cap = MobTraitCap.HOLDER.get(mob);
        Data data = cap.getOrCreateData(getRegistryName(), Data::new);
        if (!inCombat) {
            data.burstActive = true;
            data.burstUsedTicks = 0;
            return;
        }
        RangedBowAttackGoal goal = RapidFireTrait.findBowGoal(m);
        if (goal == null) return;
        if (data.baseInterval == 0) {
            try {
                data.baseInterval = ATTACK_INTERVAL_FIELD.getInt(goal);
            } catch (IllegalAccessException e) {
                return;
            }
        }
        int burstShots = 10 + 5 * lv;                                  // (10 + 5N) 発
        double rateMult = 1.0 + (0.75 + 0.013 * lv);                   // 発射速度 +(75 + 1.3n)%
        int burstInterval = Math.max(1, (int) Math.round(data.baseInterval / rateMult));
        int burstWindow = burstShots * burstInterval;
        int cooldownTicks = Math.max(20, (75 - 10 * lv) * 20);         // (75 - 10N) 秒
        try {
            if (data.burstActive) {
                if (data.burstUsedTicks < burstWindow) {
                    data.burstUsedTicks++;
                    ATTACK_INTERVAL_FIELD.setInt(goal, burstInterval);
                } else {
                    data.burstActive = false;          // burst 終了 → cooldown へ
                    data.burstUsedTicks = 0;
                    ATTACK_INTERVAL_FIELD.setInt(goal, data.baseInterval);
                }
            } else {
                data.burstUsedTicks++;                 // cooldown 計時
                ATTACK_INTERVAL_FIELD.setInt(goal, data.baseInterval);
                if (data.burstUsedTicks >= cooldownTicks) {  // cooldown 終了 → 再装填
                    data.burstActive = true;
                    data.burstUsedTicks = 0;
                }
            }
        } catch (IllegalAccessException ignored) {}
    }

    @SerialClass
    public static class Data extends CapStorageData {
        @SerialClass.SerialField public int baseInterval = 0;
        @SerialClass.SerialField public int burstUsedTicks = 0;
        @SerialClass.SerialField public boolean burstActive = true;
    }
}
