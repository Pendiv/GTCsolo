package DIV.gtcsolo.l2.trait;

import DIV.gtcsolo.l2.trait.base.TypedMobTrait;
import dev.xkmc.l2hostility.content.capability.mob.CapStorageData;
import dev.xkmc.l2hostility.content.capability.mob.MobTraitCap;
import dev.xkmc.l2serial.serialization.SerialClass;
import net.minecraft.ChatFormatting;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.RangedBowAttackGoal;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;

import java.lang.reflect.Field;

/**
 * [45] Rapid Fire (高速射撃) — スケルトン専用。
 * 戦闘継続中に射撃間隔が徐々に短縮される。
 *
 * <p>実装: {@link RangedBowAttackGoal#attackIntervalMin} 私有 field を ObfuscationReflectionHelper
 * 経由で実行時書き換え。 SRG name = {@code f_25774_}。
 *
 * <p>tick で「最初に発見した時の base 値を state に保存 → 経過 tick に応じて短縮」。
 * 最小値 = 5 tick。 短縮率 = base × (1 - (0.10 + 0.05 × lv) × min(1, sustained_tick/200))。
 *
 * <p>排他: [46] Burst Fire と排他 (連射機構衝突)、 datapack 側で配布絞り推奨。
 */
public class RapidFireTrait extends TypedMobTrait {

    private static final Field ATTACK_INTERVAL_FIELD;
    static {
        Field f = null;
        try {
            f = ObfuscationReflectionHelper.findField(RangedBowAttackGoal.class, "f_25774_");
        } catch (Exception ignored) {
            try {
                f = RangedBowAttackGoal.class.getDeclaredField("attackIntervalMin");
                f.setAccessible(true);
            } catch (Exception ignored2) { /* fail silently */ }
        }
        ATTACK_INTERVAL_FIELD = f;
    }

    private static final int MIN_INTERVAL = 5;
    private static final int RAMP_TICKS = 200;

    public RapidFireTrait(ChatFormatting style) {
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
        // ターゲット未設定 = 戦闘外 → ramp counter リセット
        boolean inCombat = m.getTarget() != null && m.getTarget().isAlive();
        MobTraitCap cap = MobTraitCap.HOLDER.get(mob);
        Data data = cap.getOrCreateData(getRegistryName(), Data::new);
        if (!inCombat) {
            data.sustainedTicks = 0;
            return;
        }
        data.sustainedTicks++;
        // base interval を保存 (初回のみ)
        RangedBowAttackGoal goal = findBowGoal(m);
        if (goal == null) return;
        if (data.baseInterval == 0) {
            try {
                data.baseInterval = ATTACK_INTERVAL_FIELD.getInt(goal);
            } catch (IllegalAccessException e) {
                return;
            }
        }
        double ramp = Math.min(1.0, data.sustainedTicks / (double) RAMP_TICKS);
        double factor = 1.0 - (0.10 + 0.05 * lv) * ramp;  // (10 + 5n)% 短縮
        int newInterval = Math.max(MIN_INTERVAL, (int) Math.round(data.baseInterval * factor));
        try {
            ATTACK_INTERVAL_FIELD.setInt(goal, newInterval);
        } catch (IllegalAccessException ignored) {}
    }

    static RangedBowAttackGoal findBowGoal(Mob mob) {
        for (var wrapped : mob.goalSelector.getAvailableGoals()) {
            Goal g = wrapped.getGoal();
            if (g instanceof RangedBowAttackGoal bow) return bow;
        }
        return null;
    }

    static Field getAttackIntervalField() { return ATTACK_INTERVAL_FIELD; }

    @SerialClass
    public static class Data extends CapStorageData {
        @SerialClass.SerialField public int baseInterval = 0;
        @SerialClass.SerialField public int sustainedTicks = 0;
    }
}
