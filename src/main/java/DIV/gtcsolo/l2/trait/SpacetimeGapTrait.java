package DIV.gtcsolo.l2.trait;

import DIV.gtcsolo.l2.trait.base.ISpacetimeTrait;
import dev.xkmc.l2damagetracker.contents.attack.AttackCache;
import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import net.minecraft.ChatFormatting;
import net.minecraft.world.entity.LivingEntity;

/**
 * 時空の隙 (Spacetime Gap) — 一定時間 (400 - 20n tick) 攻撃を受けないと全回復する。
 * 全回復に成功すると固定 1200 tick のクールタイムに入る。
 *
 * <p>被弾時刻と CD 終了時刻を world gameTime (= reload 跨ぎでも単調増加) で persistentData に保存。
 */
public class SpacetimeGapTrait extends MobTrait implements ISpacetimeTrait {

    private static final String LAST_HIT_KEY = "gtcsolo.spacetime_gap_lasthit";
    private static final String COOLDOWN_KEY = "gtcsolo.spacetime_gap_cooldown";
    private static final int HEAL_INTERVAL = 20;        // 全回復チェック間隔
    private static final int IDLE_BASE = 400;           // 無被弾 (400 - 20n) tick で全回復
    private static final int IDLE_PER_LEVEL = 20;
    private static final int IDLE_MIN = 20;             // 下限 1 秒
    private static final int HEAL_COOLDOWN = 1200;      // 全回復成功後の固定 CD

    public SpacetimeGapTrait(ChatFormatting style) {
        super(style);
    }

    private static int idleTicks(int level) {
        return Math.max(IDLE_MIN, IDLE_BASE - IDLE_PER_LEVEL * level);
    }

    @Override
    public void onDamaged(int level, LivingEntity mob, AttackCache cache) {
        super.onDamaged(level, mob, cache);
        if (mob.level().isClientSide()) return;
        mob.getPersistentData().putLong(LAST_HIT_KEY, mob.level().getGameTime());
    }

    @Override
    public void tick(LivingEntity mob, int level) {
        super.tick(mob, level);
        if (mob.level().isClientSide()) return;
        if (mob.tickCount % HEAL_INTERVAL != 0) return;
        long now = mob.level().getGameTime();
        if (now < mob.getPersistentData().getLong(COOLDOWN_KEY)) return;                      // 全回復後の CD 中
        if (now - mob.getPersistentData().getLong(LAST_HIT_KEY) < idleTicks(level)) return;   // 無被弾時間が不足
        if (mob.getHealth() < mob.getMaxHealth()) {
            mob.setHealth(mob.getMaxHealth());
            mob.getPersistentData().putLong(COOLDOWN_KEY, now + HEAL_COOLDOWN);  // 1200t 固定 CD
        }
    }
}
