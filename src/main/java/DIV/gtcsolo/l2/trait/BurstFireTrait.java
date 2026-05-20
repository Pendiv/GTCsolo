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
 * 最初の数発のみ射撃間隔が大幅短縮、 規定回数後は通常に戻る。
 *
 * <p>仕様: 立ち上がり瞬間火力。 {@link #BURST_SHOTS} = 3 + lv 発、 通常の半分以下の間隔。
 * <p>shot count は射撃検出が困難なため、 sustained tick で近似 (= 同じ target 連続戦闘で
 * BURST_SHOTS × 通常 interval ぶんが経過したら通常に戻る)。
 *
 * <p>排他: [45] Rapid Fire と排他 (連射機構衝突)、 datapack 側で配布絞り推奨。
 */
public class BurstFireTrait extends TypedMobTrait {

    private static final Field ATTACK_INTERVAL_FIELD = RapidFireTrait.getAttackIntervalField();
    private static final int BURST_INTERVAL = 6;
    private static final int BURST_SHOTS_BASE = 3;

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
        int burstShots = BURST_SHOTS_BASE + lv;
        int burstWindow = burstShots * BURST_INTERVAL;
        if (data.burstActive && data.burstUsedTicks < burstWindow) {
            data.burstUsedTicks++;
            try {
                ATTACK_INTERVAL_FIELD.setInt(goal, BURST_INTERVAL);
            } catch (IllegalAccessException ignored) {}
        } else {
            data.burstActive = false;
            try {
                ATTACK_INTERVAL_FIELD.setInt(goal, data.baseInterval);
            } catch (IllegalAccessException ignored) {}
        }
    }

    @SerialClass
    public static class Data extends CapStorageData {
        @SerialClass.SerialField public int baseInterval = 0;
        @SerialClass.SerialField public int burstUsedTicks = 0;
        @SerialClass.SerialField public boolean burstActive = true;
    }
}
